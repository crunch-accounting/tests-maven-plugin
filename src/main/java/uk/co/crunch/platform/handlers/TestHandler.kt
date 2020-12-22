package uk.co.crunch.platform.handlers

import com.google.common.collect.EnumMultiset
import com.google.common.collect.Multiset
import org.apache.maven.plugin.logging.Log
import org.objectweb.asm.Opcodes
import uk.co.crunch.platform.TestType
import uk.co.crunch.platform.api.tests.CrunchTestValidationOverrides
import uk.co.crunch.platform.asm.AnnotatedFieldVisitor
import uk.co.crunch.platform.asm.ClassAnnotationVisitor
import uk.co.crunch.platform.asm.ClassDefinitionVisitor
import uk.co.crunch.platform.asm.MethodAnnotationVisitor
import uk.co.crunch.platform.asm.MethodCallVisitor
import uk.co.crunch.platform.asm.MethodDefinitionVisitor
import uk.co.crunch.platform.exceptions.CrunchRuleViolationException
import uk.co.crunch.platform.maven.CrunchServiceMojo
import uk.co.crunch.platform.utils.InstantTimer
import java.io.File
import java.util.*

class TestHandler(private val logger: Log, private val timer: InstantTimer, private val enableAllOverrides: Boolean) : HandlerOperation {
    override fun run(mojo: CrunchServiceMojo) {
        val visitor = TestsVisitor(logger, enableAllOverrides)
        val time = timer.currentTimeMillis()
        try {
            mojo.analyseCrunchClasses({ false }, visitor)
        } finally {
            val newTime = timer.currentTimeMillis()
            logger.info("Test analysis " + visitor.langStats + " completed in " + (newTime - time) + " msecs")
            logger.info("Assertion types in use: " + visitor.assertionTypes)
        }
    }

    private class TestsVisitor(private val logger: Log, private val enableAllOverrides: Boolean) : ClassDefinitionVisitor, ClassAnnotationVisitor,
        MethodAnnotationVisitor, MethodDefinitionVisitor, MethodCallVisitor, AnnotatedFieldVisitor {
        private var isKotlin = false
        private var foundJUnit4 = false
        private var foundJUnit5 = false
        private val classLevelOverrides = EnumSet.noneOf(CrunchTestValidationOverrides::class.java)
        private val classLevelOverrideWarningShown = EnumSet.noneOf(CrunchTestValidationOverrides::class.java)
        private val publicTestMethodsPerClass = ArrayList<String>()
        private val testPrefixMethodsPerClass = ArrayList<String>()
        private val dubiousFieldsPerClass = ArrayList<String>()
        val assertionTypes: Multiset<AssertionType> = EnumMultiset.create(AssertionType::class.java)
        private var shownKotlinAssertJMigrateWarning = false
        private var shownAssertThrowsWarning = false
        val langStats: Multiset<LanguageType> = EnumMultiset.create(LanguageType::class.java)
        private var isMethodPublic = false
        private var isClassPublic = false
        private var shownClassPublicWarning = false

        override fun visitClass(access: Int, className: String, signature: String?, superName: String, interfaces: Array<String>) {
            isClassPublic = access and Opcodes.ACC_PUBLIC != 0
            publicTestMethodsPerClass.clear()
            testPrefixMethodsPerClass.clear()
            dubiousFieldsPerClass.clear()
            isKotlin = false

            if (className.endsWith("Test") && !(className.endsWith("UnitTest") || className.endsWith("IntegrationTest"))) {
                logger.warn("Unclassified test class `" + displayClassName(className) + "` should clarify whether it is a Unit or Integration test")
            }
        }

        override fun visitClassAnnotation(className: String, descriptor: String, annotationValues: Map<String, List<Any>>) {
            if (!isKotlin && descriptor.endsWith("kotlin/Metadata;")) {
                isKotlin = true
            }
            if (descriptor.endsWith("CrunchTestValidationOverride" + ";")) {
                annotationValues["value"]?.filterIsInstance(String::class.java)?.forEach {
                    classLevelOverrides.add(CrunchTestValidationOverrides.valueOf(it.substringAfterLast(".")))
                }
            }
        }

        override fun visitClassAnnotation(className: String, descriptor: String, annotationName: String, value: Any) {
            // NOOP
        }

        override fun visitMethodAnnotation(className: String, descriptor: String, name: String, value: Any?) {
            // FIXME Check for @Disabled??
            val gotJUnit5 = descriptor.contains("L" + "org/junit/jupiter/api/Test;")
            val gotJUnit4 = descriptor.contains("L" + "org/junit/Test;")

            if (gotJUnit4 || gotJUnit5) {
                if (foundJUnit4 && gotJUnit5 || foundJUnit5 && gotJUnit4) {
                    handleViolation(CrunchTestValidationOverrides.MIXED_JUNIT4_JUNIT5) { "Cannot combine JUnit 4 and JUnit 5 tests! See: $className" }
                }

                foundJUnit4 = foundJUnit4 or gotJUnit4
                foundJUnit5 = foundJUnit5 or gotJUnit5

                if (isKotlin) {
//                    this.logger.debug("Kotlin Test: " + className + " : " + name);
                    langStats.add(LanguageType.Kotlin)
                } else {
//                    this.logger.debug("Java Test: " + className + " : " + name);
                    langStats.add(LanguageType.Java)
                }

                if (isKotlin) {
                    if (assertionTypes.contains(AssertionType.AssertJ) && !shownKotlinAssertJMigrateWarning) {
                        logger.warn("Kotlin tests ought to start moving from AssertJ => Strikt, where possible")
                        shownKotlinAssertJMigrateWarning = true
                    }
                }
                if (!shownClassPublicWarning && isClassPublic && gotJUnit5 && !isKotlin) {
                    logger.warn("Java test class `" + displayClassName(className) + "` does not need to be public")
                    shownClassPublicWarning = true
                }

                // JUnit5 test methods don't need to be public (irrelevant for Kotlin)
                if (gotJUnit5) {
                    if (!isKotlin && isMethodPublic) {
                        publicTestMethodsPerClass.add(name)
                    }
                    if (name.startsWith("test")) {
                        testPrefixMethodsPerClass.add(name)
                    }
                }
            }
        }

        override fun visitMethodCall(className: String, owner: String, name: String, desc: String) {
            if (owner == "org/junit/Assert") {
                assertionTypes.add(AssertionType.JUnit4)
                handleViolation(CrunchTestValidationOverrides.JUNIT4_ASSERTIONS) {
                    "We should stop using JUnit4 assertions (" + displayClassName(
                        className
                    ) + "." + name + ")"
                }
            } else if (owner == "junit/framework/TestCase") {
                assertionTypes.add(AssertionType.JUnit3)
                handleViolation(CrunchTestValidationOverrides.JUNIT3_ASSERTIONS) {
                    "We should stop using JUnit3 assertions (" + displayClassName(
                        className
                    ) + "." + name + ")"
                }
            } else if (owner == "org/junit/jupiter/api/Assertions") {
                assertionTypes.add(AssertionType.JUnit5)
                if (name == "assertThrows" || name == "assertDoesNotThrow") {
                    if (!shownAssertThrowsWarning) {
                        logger.info("JUnit5 assertThrows() can be replaced by AssertJ too: https://www.baeldung.com/assertj-exception-assertion")
                        shownAssertThrowsWarning = true
                    }
                } else {
                    handleViolation(CrunchTestValidationOverrides.JUNIT5_ASSERTIONS) {
                        "We should stop using JUnit5 assertions (" + displayClassName(
                            className
                        ) + "." + name + ")"
                    }
                }
            } else if (owner.startsWith("org/hamcrest/MatcherAssert")) {
                assertionTypes.add(AssertionType.Hamcrest)
                handleViolation(CrunchTestValidationOverrides.HAMCREST_USAGE) { "We should stop using Hamcrest (" + displayClassName(className) + "." + name + ")" }
            } else if (owner.startsWith("strikt/api")) {
                assertionTypes.add(AssertionType.Strikt)
            } else if (owner.startsWith("org/assertj/core/api/Assertions")) {
                assertionTypes.add(AssertionType.AssertJ)
            } else if (owner.startsWith("com/google/common/truth/Truth")) {
                assertionTypes.add(AssertionType.Truth)
                handleViolation(CrunchTestValidationOverrides.TRUTH_ASSERTIONS) {
                    "We should stop using Google Truth assertions (" + displayClassName(
                        className
                    ) + "." + name + ")"
                }
            }
        }

        override fun finishedVisitingField(
            className: String, testType: TestType,
            fieldAccess: Int, fieldName: String, fieldDescriptor: String, fieldSignature: String?,
            annotationsForField: List<String>
        ) {
            if (!className.endsWith("Test.class")) {
                return  // Skip non-tests
            }
            if (fieldAccess and Opcodes.ACC_FINAL != 0 && fieldAccess and Opcodes.ACC_STATIC != 0) {
                return  // Skip final statics
            }
            if (testType == TestType.Mixed) {
                logger.info("Can't validate fields for unclear test type")
                return  // Skip mocks
            }
            if (testType == TestType.Unit && annotationsForField.any { it.startsWith("Lorg/mockito/") }) {
                return  // Skip mocks
            }
            if (testType == TestType.Integration &&
                (annotationsForField.contains("Lorg/springframework/beans/factory/annotation/Autowired;") ||
                    annotationsForField.contains("Lorg/springframework/beans/factory/annotation/Value;") ||
                    annotationsForField.any { it.startsWith("Lorg/springframework/boot/test/mock") || it.startsWith("Lcom/ninjasquad/springmockk") })
            ) {
                return  // Skip injection stuff
            }
            dubiousFieldsPerClass.add(fieldName)
            // System.out.println(className + ": " + className + " / " + fieldDescriptor + " / " + fieldSignature + " : " + annotationsForField);
        }

        private fun handleViolation(override: CrunchTestValidationOverrides, handler: ViolationHandler) {
            if (enableAllOverrides || isOverridden(override)) {
                if (!classLevelOverrideWarningShown.contains(override)) {
                    logger.warn(handler.getMessage())
                    classLevelOverrideWarningShown.add(override)
                }
            } else {
                throw CrunchRuleViolationException(handler.getMessage())
            }
        }

        private fun isOverridden(override: CrunchTestValidationOverrides) = classLevelOverrides.contains(override) // FIXME method-level too!

        fun interface ViolationHandler {
            fun getMessage(): String?
        }

        override fun visitMethod(access: Int, methodName: String, desc: String, signature: String?, exceptions: Array<String>?) {
            isMethodPublic = access and Opcodes.ACC_PUBLIC != 0
        }

        override fun finishedVisitingClass(className: String, testType: TestType) {
            if (publicTestMethodsPerClass.isNotEmpty()) {
                logger.warn(testType.toString() + " test class `" + displayClassName(className) + "`, methods: " + publicTestMethodsPerClass + " do not need to be public")
            }
            if (testPrefixMethodsPerClass.isNotEmpty()) {
                logger.warn(testType.toString() + " test class `" + displayClassName(className) + "`, methods: " + testPrefixMethodsPerClass + " do not need the prefix 'test'")
            }
            if (dubiousFieldsPerClass.isNotEmpty()) {
                logger.warn(testType.toString() + " test class `" + displayClassName(className) + "`, fields: " + dubiousFieldsPerClass + " are dubious")
            }
        }
    }

    @Suppress("SpellCheckingInspection")
    private enum class AssertionType {
        Hamcrest, AssertJ, JUnit3, JUnit4, JUnit5, Strikt, Truth
    }

    private enum class LanguageType {
        Java, Kotlin
    }

    companion object {
        private fun displayClassName(className: String) = className.removeSuffix(".class").replace(File.separatorChar, '.')
    }
}
