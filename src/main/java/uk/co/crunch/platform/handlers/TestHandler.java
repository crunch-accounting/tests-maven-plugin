package uk.co.crunch.platform.handlers;

import uk.co.crunch.platform.asm.ClassAnnotationVisitor;
import uk.co.crunch.platform.asm.MethodAnnotationVisitor;
import uk.co.crunch.platform.maven.CrunchServiceMojo;

public class TestHandler implements HandlerOperation {
    @Override
    public void run(CrunchServiceMojo mojo) {

        // TODO: Warn about "test" prefix.
        // TODO: Warn about public class and test methods
        mojo.analyseCrunchClasses(() -> false, new Vis(mojo));
    }

    private static class Vis implements MethodAnnotationVisitor, ClassAnnotationVisitor {

        private final CrunchServiceMojo mojo;

        private boolean isKotlin = false;

        public Vis(CrunchServiceMojo mojo) {
            this.mojo = mojo;
        }

        @Override
        public void visitClassAnnotation(String className, String descriptor) {
            if (descriptor.endsWith("kotlin/Metadata;")) {
                isKotlin = true;
            }
        }

        @Override
        public void visit(String className, String descriptor, String name, Object value) {
            // FIXME Check for @Disabled??
            if (matchesApiClassName(descriptor)) {
                if (isKotlin) {
                    mojo.getLog().info("Kotlin Test: " + className + " : " + name);
                } else {
                    mojo.getLog().info("Java Test: " + className + " : " + name);
                }
            }
        }
    }

    private static boolean matchesApiClassName(String desc) {
        return desc.contains("L" + "org/junit/jupiter/api/Test;") ||
                desc.contains("L" + "org/junit/Test;");
    }
}
