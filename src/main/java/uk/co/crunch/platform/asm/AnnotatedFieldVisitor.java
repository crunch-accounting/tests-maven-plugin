package uk.co.crunch.platform.asm;

import java.util.List;

import uk.co.crunch.platform.TestType;

@FunctionalInterface
public interface AnnotatedFieldVisitor {
    void finishedVisitingField(String className, TestType testType, int fieldAccess, String fieldName, String fieldDescriptor, String fieldSignature, List<String> annotationsForField);
}
