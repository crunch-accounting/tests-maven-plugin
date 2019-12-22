package uk.co.crunch.platform.asm;

@FunctionalInterface
public interface MethodAnnotationVisitor extends AsmVisitor {
    void visitMethodAnnotation(String className, String descriptor, String name, Object value);
}
