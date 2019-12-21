package uk.co.crunch.platform.asm;

import java.nio.file.Path;

@FunctionalInterface
public interface VirtualMethodVisitor extends AsmVisitor {
    void visit(String className, String owner, String name, String desc);
}
