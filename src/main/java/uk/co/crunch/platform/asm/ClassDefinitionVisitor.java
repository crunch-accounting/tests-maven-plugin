package uk.co.crunch.platform.asm;

@SuppressWarnings("unused")
public interface ClassDefinitionVisitor extends AsmVisitor {
    void visitClass(int access, String name, String signature, String superName, String[] interfaces);
    void finishedVisitingClass(String name);
}
