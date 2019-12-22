package uk.co.crunch.platform.asm;

@FunctionalInterface
public interface MethodDefinitionVisitor extends AsmVisitor {
    void visitMethod(int access, String methodName, String desc, String signature, String[] exceptions);
}
