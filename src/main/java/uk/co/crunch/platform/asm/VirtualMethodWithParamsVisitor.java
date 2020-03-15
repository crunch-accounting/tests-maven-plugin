package uk.co.crunch.platform.asm;

import java.nio.file.Path;
import java.util.ArrayList;

@FunctionalInterface
public interface VirtualMethodWithParamsVisitor extends AsmVisitor {
    void visit(Path eachClass, String methodName, String owner, String name, String desc, ArrayList<String> ldcs);
}
