package uk.co.crunch.platform.asm;

import java.nio.file.Path;

@FunctionalInterface
public interface VirtualMethodVisitor extends AsmVisitor {
    void visit(Path eachClass, String owner, String name, String desc);
}
