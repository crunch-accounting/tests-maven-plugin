package uk.co.crunch.platform.handlers;

import com.google.common.collect.EnumMultiset;
import com.google.common.collect.Multiset;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Opcodes;
import uk.co.crunch.platform.api.tests.CrunchTestValidationOverrides;
import uk.co.crunch.platform.asm.*;
import uk.co.crunch.platform.exceptions.CrunchRuleViolationException;
import uk.co.crunch.platform.maven.CrunchServiceMojo;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static uk.co.crunch.platform.api.tests.CrunchTestValidationOverrides.*;

public class TestHandler implements HandlerOperation {
    @Override
    public void run(CrunchServiceMojo mojo) {

        var visitor = new TestsVisitor(mojo);
        var time = System.currentTimeMillis();
        try {
            mojo.analyseCrunchClasses(() -> false, visitor);
        } finally {
            var newTime = System.currentTimeMillis();
            mojo.getLog().info("Test analysis " + visitor.langStats + " completed in " + (newTime - time) + " msecs");
            mojo.getLog().info("Assertion types in use: " + visitor.assertionTypes);
        }
    }

    private static class TestsVisitor implements ClassDefinitionVisitor, ClassAnnotationVisitor, MethodAnnotationVisitor, MethodDefinitionVisitor, MethodCallVisitor {

        private final CrunchServiceMojo mojo;

        private boolean isKotlin;
        private boolean foundJUnit4;
        private boolean foundJUnit5;

        private final EnumSet<CrunchTestValidationOverrides> classLevelOverrides = EnumSet.noneOf(CrunchTestValidationOverrides.class);
        private final EnumSet<CrunchTestValidationOverrides> classLevelOverrideWarningShown = EnumSet.noneOf(CrunchTestValidationOverrides.class);

        private List<String> publicTestMethodsPerClass = new ArrayList<>();
        private List<String> testPrefixMethodsPerClass = new ArrayList<>();

        private final Multiset<AssertionType> assertionTypes = EnumMultiset.create(AssertionType.class);
        private boolean shownKotlinAssertJMigrateWarning;

        private final Multiset<LanguageType> langStats = EnumMultiset.create(LanguageType.class);

        private boolean isMethodPublic;
        private boolean isMethodPrivate;

        private boolean isClassPublic;
        private boolean shownClassPublicWarning;

        public TestsVisitor(CrunchServiceMojo mojo) {
            this.mojo = mojo;
        }

        @Override
        public void visitClass(int access, String className, String signature, String superName, String[] interfaces) {
            this.isClassPublic = ((access & Opcodes.ACC_PUBLIC) != 0);
            this.publicTestMethodsPerClass.clear();
            this.testPrefixMethodsPerClass.clear();

            if (className.endsWith("Test") && !(className.endsWith("UnitTest") || className.endsWith("IntegrationTest"))) {
                mojo.getLog().warn("Unclassified test class `" + displayClassName(className) + "` should clarify whether it is a Unit or Integration test");
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
//                    mojo.getLog().debug("Kotlin Test: " + className + " : " + name);
                    this.langStats.add(LanguageType.Kotlin);
                } else {
//                    mojo.getLog().debug("Java Test: " + className + " : " + name);
                    this.langStats.add(LanguageType.Java);
                }

                if (this.assertionTypes.contains(AssertionType.JUnit4)) {
                    handleViolation(JUNIT4_ASSERTIONS, () -> "We should stop using JUnit4 assertions (" + displayClassName(className) + "." + name + ")");
                }

                if (this.assertionTypes.contains(AssertionType.Hamcrest)) {
                    handleViolation(HAMCREST_USAGE, () -> "We should stop using Hamcrest (" + displayClassName(className) + "." + name + ")");
                }

                if (isKotlin) {
                    if (this.assertionTypes.contains(AssertionType.AssertJ) && !this.shownKotlinAssertJMigrateWarning) {
                        mojo.getLog().warn("Kotlin tests ought to start moving from AssertJ => Strikt, where possible");
                        this.shownKotlinAssertJMigrateWarning = true;
                    }
                }

                if (!this.shownClassPublicWarning && isClassPublic && !isKotlin) {
                    mojo.getLog().warn("Java test class `" + displayClassName(className) + "` does not need to be public");
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

        private void handleViolation(CrunchTestValidationOverrides override, ViolationHandler handler) {
            if (isOverridden(override)) {
                if (!this.classLevelOverrideWarningShown.contains(override)) {
                    mojo.getLog().warn(handler.getMessage());
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
            isMethodPublic = isMethodPrivate = false;

            if ((access & Opcodes.ACC_PUBLIC) != 0) {
                isMethodPublic = true;
            } else if ((access & Opcodes.ACC_PRIVATE) != 0) {
                isMethodPrivate = true;
            }
        }

        @Override
        public void finishedVisitingClass(String className) {
            if (!this.publicTestMethodsPerClass.isEmpty()) {
                mojo.getLog().warn("Test class `" + displayClassName(className) + "`, methods: " + this.publicTestMethodsPerClass + " do not need to be public");
                this.publicTestMethodsPerClass.clear();
            }

            if (!this.testPrefixMethodsPerClass.isEmpty()) {
                mojo.getLog().warn("Test class `" + displayClassName(className) + "`, methods: " + this.testPrefixMethodsPerClass + " do not need the prefix 'test'");
                this.testPrefixMethodsPerClass.clear();
            }
        }

        @Override
        public void visitMethodCall(String className, String owner, String name, String desc) {

            if (owner.equals("org/junit/Assert")) {
                this.assertionTypes.add(AssertionType.JUnit4);
            }

            if (owner.startsWith("org/hamcrest/MatcherAssert")) {
                this.assertionTypes.add(AssertionType.Hamcrest);
            }

            if (owner.startsWith("strikt/api")) {
                this.assertionTypes.add(AssertionType.Strikt);
            }

            if (owner.startsWith("org/assertj")) {
                this.assertionTypes.add(AssertionType.AssertJ);
            }
        }
    }

    private enum AssertionType {
        Hamcrest, AssertJ, JUnit4, Strikt
    }

    private enum LanguageType {
        Java, Kotlin
    }

    private static String displayClassName(String className) {
        return StringUtils.stripEnd(className, ".class").replace(File.separatorChar, '.');
    }
}
