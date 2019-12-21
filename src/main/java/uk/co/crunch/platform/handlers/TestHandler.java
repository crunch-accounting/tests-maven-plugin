package uk.co.crunch.platform.handlers;

import uk.co.crunch.platform.asm.AsmVisitor;
import uk.co.crunch.platform.asm.MethodAnnotationVisitor;
import uk.co.crunch.platform.maven.CrunchServiceMojo;

public class TestHandler implements HandlerOperation {
    @Override
    public void run(CrunchServiceMojo mojo) {

        // FIXME Check for @Disabled??
        final AsmVisitor testVisitor = (MethodAnnotationVisitor) (String className, String descriptor, String name, Object value) -> {
            if (matchesApiClassName(descriptor)) {
                mojo.getLog().info("Test: " + className + " : " + name);
            }
        };

        // TODO: Warn about "test" prefix.
        mojo.analyseCrunchClasses(() -> false, testVisitor);
    }

    private static boolean matchesApiClassName(String desc) {
        return desc.contains("L" + "org/junit/jupiter/api/Test;") ||
                desc.contains("L" + "org/junit/Test;");
    }
}
