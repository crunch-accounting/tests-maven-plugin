package uk.co.crunch.platform.asm;

import java.util.List;
import java.util.Map;

public interface ClassAnnotationVisitor extends AsmVisitor {
    void visitClassAnnotation(String className, String descriptor, String annotationName, Object value);
    void visitClassAnnotation(String className, String descriptor, Map<String, List<Object>> annotationValues);

}
