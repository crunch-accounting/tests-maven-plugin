package uk.co.crunch.platform.handlers;

import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Opcodes;
import uk.co.crunch.platform.asm.ClassAnnotationVisitor;
import uk.co.crunch.platform.asm.ClassDefinitionVisitor;
import uk.co.crunch.platform.asm.MethodAnnotationVisitor;
import uk.co.crunch.platform.asm.MethodDefinitionVisitor;
import uk.co.crunch.platform.exceptions.CrunchRuleViolationException;
import uk.co.crunch.platform.maven.CrunchServiceMojo;

public class TestHandler implements HandlerOperation {
    @Override
    public void run(CrunchServiceMojo mojo) {

        var time = System.currentTimeMillis();
        try {
            mojo.analyseCrunchClasses(() -> false, new Vis(mojo));
        } finally {
            var newTime = System.currentTimeMillis();
            mojo.getLog().info("Test analysis completed in " + (newTime - time) + " msecs");
        }
    }

    private static class Vis implements ClassDefinitionVisitor, ClassAnnotationVisitor, MethodAnnotationVisitor, MethodDefinitionVisitor {

        private final CrunchServiceMojo mojo;

        private boolean isKotlin;
        private boolean foundJUnit4;
        private boolean foundJUnit5;
        private boolean isMethodPublic;
        private boolean isMethodPrivate;

        private boolean isClassPublic;
        private boolean shownClassPublicWarning;

        public Vis(CrunchServiceMojo mojo) {
            this.mojo = mojo;
        }

        @Override
        public void visitClass(int access, String name, String signature, String superName, String[] interfaces) {
            isClassPublic = ((access & Opcodes.ACC_PUBLIC) != 0);
        }

        @Override
        public void visitClassAnnotation(String className, String descriptor) {
            if (descriptor.endsWith("kotlin/Metadata;")) {
                this.isKotlin = true;
            }
        }

        @Override
        public void visitMethodAnnotation(String className, String descriptor, String name, Object value) {
            // FIXME Check for @Disabled??

            var gotJUnit5 = descriptor.contains("L" + "org/junit/jupiter/api/Test;");
            var gotJUnit4 = descriptor.contains("L" + "org/junit/Test;");

            if (gotJUnit4 || gotJUnit5) {

                if (this.foundJUnit4 && gotJUnit5 || this.foundJUnit5 && gotJUnit4) {
                    throw new CrunchRuleViolationException("Cannot combine JUnit 4 and JUnit 5 tests! See: " + className);
                }

                this.foundJUnit4 |= gotJUnit4;
                this.foundJUnit5 |= gotJUnit5;

                if (isKotlin) {
                    mojo.getLog().info("Kotlin Test: " + className + " : " + name);
                } else {
                    mojo.getLog().info("Java Test: " + className + " : " + name);
                }

                if (!this.shownClassPublicWarning && isClassPublic && !isKotlin) {
                    mojo.getLog().warn("Java test class `" + displayClassName(className) + "` does not need to be public");
                    this.shownClassPublicWarning = true;
                }

                // JUnit5 test methods don't need to be public (irrelevant for Kotlin)
                if (!isKotlin && gotJUnit5 && isMethodPublic) {
                    mojo.getLog().warn("Test `" + displayClassName(className) + "." + name + "` does not need to be public");
                }
            }
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
    }

    private static String displayClassName(String className) {
        return StringUtils.stripEnd(className, ".class");
    }
}
