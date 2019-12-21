package uk.co.crunch.platform.asm;

@FunctionalInterface
public interface ClassAnnotationVisitor extends AsmVisitor {
    void visitClassAnnotation(String className, String descriptor);
}
