package uk.co.crunch.platform.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.google.common.collect.EnumMultiset;
import com.google.common.collect.Multiset;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.objectweb.asm.Opcodes;
import uk.co.crunch.platform.TestType;
import uk.co.crunch.platform.api.tests.CrunchTestValidationOverrides;
import uk.co.crunch.platform.asm.AnnotatedFieldVisitor;
import uk.co.crunch.platform.asm.ClassAnnotationVisitor;
import uk.co.crunch.platform.asm.ClassDefinitionVisitor;
import uk.co.crunch.platform.asm.MethodAnnotationVisitor;
import uk.co.crunch.platform.asm.MethodCallVisitor;
import uk.co.crunch.platform.asm.MethodDefinitionVisitor;
import uk.co.crunch.platform.exceptions.CrunchRuleViolationException;
import uk.co.crunch.platform.maven.CrunchServiceMojo;
import uk.co.crunch.platform.utils.InstantTimer;

import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static uk.co.crunch.platform.api.tests.CrunchTestValidationOverrides.HAMCREST_USAGE;
import static uk.co.crunch.platform.api.tests.CrunchTestValidationOverrides.JUNIT3_ASSERTIONS;
import static uk.co.crunch.platform.api.tests.CrunchTestValidationOverrides.JUNIT4_ASSERTIONS;
import static uk.co.crunch.platform.api.tests.CrunchTestValidationOverrides.JUNIT5_ASSERTIONS;
import static uk.co.crunch.platform.api.tests.CrunchTestValidationOverrides.MIXED_JUNIT4_JUNIT5;
import static uk.co.crunch.platform.api.tests.CrunchTestValidationOverrides.TRUTH_ASSERTIONS;

public class TestHandler implements HandlerOperation {

    private final Log logger;
    private final InstantTimer timer;
    private final boolean enableAllOverrides;

    public TestHandler(Log logger, InstantTimer timer, boolean enableAllOverrides) {
        this.logger = logger;
        this.timer = timer;
        this.enableAllOverrides = enableAllOverrides;
    }

    @Override
    public void run(CrunchServiceMojo mojo) {

        var visitor = new TestsVisitor(this.logger, this.enableAllOverrides);
        var time = this.timer.currentTimeMillis();
        try {
            mojo.analyseCrunchClasses(() -> false, visitor);
        } finally {
            var newTime = this.timer.currentTimeMillis();
            this.logger.info("Test analysis " + visitor.langStats + " completed in " + (newTime - time) + " msecs");
            this.logger.info("Assertion types in use: " + visitor.assertionTypes);
        }
    }

    private static class TestsVisitor implements ClassDefinitionVisitor, ClassAnnotationVisitor, MethodAnnotationVisitor, MethodDefinitionVisitor, MethodCallVisitor, AnnotatedFieldVisitor {

        private final Log logger;
        private final boolean enableAllOverrides;

        private boolean isKotlin;
        private boolean foundJUnit4;
        private boolean foundJUnit5;

        private final EnumSet<CrunchTestValidationOverrides> classLevelOverrides = EnumSet.noneOf(CrunchTestValidationOverrides.class);
        private final EnumSet<CrunchTestValidationOverrides> classLevelOverrideWarningShown = EnumSet.noneOf(CrunchTestValidationOverrides.class);

        private final List<String> publicTestMethodsPerClass = new ArrayList<>();
        private final List<String> testPrefixMethodsPerClass = new ArrayList<>();
        private final List<String> dubiousFieldsPerClass = new ArrayList<>();

        private final Multiset<AssertionType> assertionTypes = EnumMultiset.create(AssertionType.class);
        private boolean shownKotlinAssertJMigrateWarning;
        private boolean shownAssertThrowsWarning;

        private final Multiset<LanguageType> langStats = EnumMultiset.create(LanguageType.class);

        private boolean isMethodPublic;

        private boolean isClassPublic;
        private boolean shownClassPublicWarning;

        public TestsVisitor(Log logger, boolean enableAllOverrides) {
            this.logger = logger;
            this.enableAllOverrides = enableAllOverrides;
        }

        @Override
        public void visitClass(int access, String className, String signature, String superName, String[] interfaces) {
            this.isClassPublic = ((access & Opcodes.ACC_PUBLIC) != 0);
            this.publicTestMethodsPerClass.clear();
            this.testPrefixMethodsPerClass.clear();
            this.dubiousFieldsPerClass.clear();

            if (className.endsWith("Test") && !(className.endsWith("UnitTest") || className.endsWith("IntegrationTest"))) {
                this.logger.warn("Unclassified test class `" + displayClassName(className) + "` should clarify whether it is a Unit or Integration test");
            }
        }

        @Override
        public void visitClassAnnotation(String className, String descriptor, Map<String, List<Object>> annotationValues) {
            if (!this.isKotlin && descriptor.endsWith("kotlin/Metadata;")) {
                this.isKotlin = true;
            }

            if (descriptor.endsWith("CrunchTestValidationOverride" + ";")) {
                annotationValues.get("value").forEach(name ->
                    this.classLevelOverrides.add(CrunchTestValidationOverrides.valueOf(substringAfterLast((String) name, "."))));
            }
        }

        @Override
        public void visitClassAnnotation(String className, String descriptor, String annotationName, Object value) {
            // NOOP
        }

        @Override
        public void visitMethodAnnotation(String className, String descriptor, String name, Object value) {
            // FIXME Check for @Disabled??

            var gotJUnit5 = descriptor.contains("L" + "org/junit/jupiter/api/Test;");
            var gotJUnit4 = descriptor.contains("L" + "org/junit/Test;");

            if (gotJUnit4 || gotJUnit5) {

                if (this.foundJUnit4 && gotJUnit5 || this.foundJUnit5 && gotJUnit4) {
                    handleViolation(MIXED_JUNIT4_JUNIT5, () -> "Cannot combine JUnit 4 and JUnit 5 tests! See: " + className);
                }

                this.foundJUnit4 |= gotJUnit4;
                this.foundJUnit5 |= gotJUnit5;

                if (isKotlin) {
//                    this.logger.debug("Kotlin Test: " + className + " : " + name);
                    this.langStats.add(LanguageType.Kotlin);
                } else {
//                    this.logger.debug("Java Test: " + className + " : " + name);
                    this.langStats.add(LanguageType.Java);
                }

                if (isKotlin) {
                    if (this.assertionTypes.contains(AssertionType.AssertJ) && !this.shownKotlinAssertJMigrateWarning) {
                        this.logger.warn("Kotlin tests ought to start moving from AssertJ => Strikt, where possible");
                        this.shownKotlinAssertJMigrateWarning = true;
                    }
                }

                if (!this.shownClassPublicWarning && isClassPublic && gotJUnit5 && !isKotlin) {
                    this.logger.warn("Java test class `" + displayClassName(className) + "` does not need to be public");
                    this.shownClassPublicWarning = true;
                }

                // JUnit5 test methods don't need to be public (irrelevant for Kotlin)
                if (gotJUnit5) {
                    if (!isKotlin && isMethodPublic) {
                        this.publicTestMethodsPerClass.add(name);
                    }

                    if (name.startsWith("test")) {
                        this.testPrefixMethodsPerClass.add(name);
                    }
                }
            }
        }

        @Override
        public void visitMethodCall(String className, String owner, String name, String desc) {

            if (owner.equals("org/junit/Assert")) {
                this.assertionTypes.add(AssertionType.JUnit4);
                handleViolation(JUNIT4_ASSERTIONS, () -> "We should stop using JUnit4 assertions (" + displayClassName(className) + "." + name + ")");
            } else if (owner.equals("junit/framework/TestCase")) {
                this.assertionTypes.add(AssertionType.JUnit3);
                handleViolation(JUNIT3_ASSERTIONS, () -> "We should stop using JUnit3 assertions (" + displayClassName(className) + "." + name + ")");
            } else if (owner.equals("org/junit/jupiter/api/Assertions")) {
                this.assertionTypes.add(AssertionType.JUnit5);

                if (name.equals("assertThrows") || name.equals("assertDoesNotThrow")) {
                    if (!this.shownAssertThrowsWarning) {
                        this.logger.info("JUnit5 assertThrows() can be replaced by AssertJ too: https://www.baeldung.com/assertj-exception-assertion");
                        this.shownAssertThrowsWarning = true;
                    }
                } else {
                    handleViolation(JUNIT5_ASSERTIONS, () -> "We should stop using JUnit5 assertions (" + displayClassName(className) + "." + name + ")");
                }

            } else if (owner.startsWith("org/hamcrest/MatcherAssert")) {
                this.assertionTypes.add(AssertionType.Hamcrest);
                handleViolation(HAMCREST_USAGE, () -> "We should stop using Hamcrest (" + displayClassName(className) + "." + name + ")");
            } else if (owner.startsWith("strikt/api")) {
                this.assertionTypes.add(AssertionType.Strikt);
            } else if (owner.startsWith("org/assertj/core/api/Assertions")) {
                this.assertionTypes.add(AssertionType.AssertJ);
            } else if (owner.startsWith("com/google/common/truth/Truth")) {
                this.assertionTypes.add(AssertionType.Truth);
                handleViolation(TRUTH_ASSERTIONS, () -> "We should stop using Google Truth assertions (" + displayClassName(className) + "." + name + ")");
            }
        }

        @Override
        public void finishedVisitingField(String className, TestType testType,
                                          int fieldAccess, String fieldName, String fieldDescriptor, String fieldSignature,
                                          List<String> annotationsForField) {

            if (!className.endsWith("Test.class")) {
                return;  // Skip non-tests
            }

            if ((fieldAccess & Opcodes.ACC_FINAL) != 0 && (fieldAccess & Opcodes.ACC_STATIC) != 0) {
                return;  // Skip final statics
            }

            if (testType == TestType.Mixed) {
                this.logger.info("Can't validate fields for unclear test type");
                return;  // Skip mocks
            }

            if (testType == TestType.Unit && annotationsForField.stream().anyMatch(x -> x.startsWith("Lorg/mockito/"))) {
                return;  // Skip mocks
            }

            if (testType == TestType.Integration &&
                (annotationsForField.contains("Lorg/springframework/beans/factory/annotation/Autowired;") ||
                    annotationsForField.contains("Lorg/springframework/beans/factory/annotation/Value;") ||
                    annotationsForField.stream().anyMatch(x -> x.startsWith("Lorg/springframework/boot/test/mock")))) {
                return;  // Skip injection stuff
            }

            this.dubiousFieldsPerClass.add(fieldName);
//            System.out.println(className + ": " + className + " / " + fieldDescriptor + " / " + fieldSignature + " : " + annotationsForField);
        }

        private void handleViolation(CrunchTestValidationOverrides override, ViolationHandler handler) {
            if (this.enableAllOverrides || isOverridden(override)) {
                if (!this.classLevelOverrideWarningShown.contains(override)) {
                    this.logger.warn(handler.getMessage());
                    this.classLevelOverrideWarningShown.add(override);
                }
            } else {
                throw new CrunchRuleViolationException(handler.getMessage());
            }
        }

        private boolean isOverridden(CrunchTestValidationOverrides override) {
            // FIXME method-level too!
            return this.classLevelOverrides.contains(override);
        }

        @FunctionalInterface
        interface ViolationHandler {
            String getMessage();
        }

        @Override
        public void visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
            isMethodPublic = (access & Opcodes.ACC_PUBLIC) != 0;
        }

        @Override
        public void finishedVisitingClass(String className, TestType testType) {
            if (!this.publicTestMethodsPerClass.isEmpty()) {
                this.logger.warn(testType + " test class `" + displayClassName(className) + "`, methods: " + this.publicTestMethodsPerClass + " do not need to be public");
            }

            if (!this.testPrefixMethodsPerClass.isEmpty()) {
                this.logger.warn(testType + " test class `" + displayClassName(className) + "`, methods: " + this.testPrefixMethodsPerClass + " do not need the prefix 'test'");
            }

            if (!this.dubiousFieldsPerClass.isEmpty()) {
                this.logger.warn(testType + " test class `" + displayClassName(className) + "`, fields: " + this.dubiousFieldsPerClass + " are dubious");
            }
        }
    }

    private enum AssertionType {
        Hamcrest, AssertJ, JUnit3, JUnit4, JUnit5, Strikt, Truth
    }

    private enum LanguageType {
        Java, Kotlin
    }

    private static String displayClassName(String className) {
        return StringUtils.stripEnd(className, ".class").replace(File.separatorChar, '.');
    }
}
