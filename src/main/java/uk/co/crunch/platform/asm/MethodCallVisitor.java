package uk.co.crunch.platform.asm;

@FunctionalInterface
public interface MethodCallVisitor extends AsmVisitor {
    void visitMethodCall(String className, String owner, String name, String desc);
}
