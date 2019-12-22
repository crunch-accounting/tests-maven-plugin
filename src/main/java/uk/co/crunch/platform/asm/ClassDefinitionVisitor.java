package uk.co.crunch.platform.asm;

@FunctionalInterface
public interface ClassDefinitionVisitor extends AsmVisitor {
    void visitClass(int access, String name, String signature, String superName, String[] interfaces);
}
