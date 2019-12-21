package uk.co.crunch.platform.asm;

@FunctionalInterface
public interface MethodAnnotationVisitor extends AsmVisitor {
    void visit(String descriptor, String name, Object value);
}
