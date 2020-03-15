package uk.co.crunch.platform.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import uk.co.crunch.platform.TestType;
import uk.co.crunch.platform.asm.AnnotatedFieldVisitor;
import uk.co.crunch.platform.asm.AsmVisitor;
import uk.co.crunch.platform.asm.AsmVisitor.DoneCheck;
import uk.co.crunch.platform.asm.ClassAnnotationVisitor;
import uk.co.crunch.platform.asm.ClassDefinitionVisitor;
import uk.co.crunch.platform.asm.MethodAnnotationVisitor;
import uk.co.crunch.platform.asm.MethodCallVisitor;
import uk.co.crunch.platform.asm.MethodDefinitionVisitor;
import uk.co.crunch.platform.asm.VirtualMethodWithParamsVisitor;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.stripEnd;

public class AsmUtils {

    private final static int API_VERSION = Opcodes.ASM7;
    private static final String CRUNCH_PACKAGE_PATH = "uk/co/crunch/".replace("/", File.separator);

    public static void visitCrunchClasses(final String classPathEntry,
                                          final DoneCheck doneCheck,
                                          final AsmVisitor... handlers) throws IOException {

        final File classesDir = new File(classPathEntry);
        if (!classesDir.isDirectory()) {
            return;
        }

        try (Stream<Path> classPathStream = Files.walk(classesDir.toPath())) {
            for (Path eachClass : classPathStream
                .filter(file -> file.toString().endsWith(".class") && file.toString().contains(CRUNCH_PACKAGE_PATH))
                .sorted()  // Seems to be issue with Maven 3.3.x vs Maven 3.5
                .collect(toList())) {

                try (InputStream theStream = Files.newInputStream(eachClass)) {

                    var className = eachClass.getFileName().toString();
                    var testType = className.endsWith("IntegrationTest.class") ? TestType.Integration : (className.endsWith("UnitTest.class") ? TestType.Unit : TestType.Mixed);

                    new ClassReader(theStream).accept(new ClassVisitor(API_VERSION) {

                        @Override
                        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                            for (ClassDefinitionVisitor handler : filterHandlers(ClassDefinitionVisitor.class, handlers).collect(toList())) {
                                handler.visitClass(access, name, signature, superName, interfaces);
                            }
                        }

                        @Override
                        public void visitEnd() {
                            for (ClassDefinitionVisitor handler : filterHandlers(ClassDefinitionVisitor.class, handlers).collect(toList())) {
                                handler.finishedVisitingClass(className, testType);
                            }
                        }

                        @Override
                        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                            // TODO Ensure this is actually a test class, e.g. not a helper / Utils

                            var annotationsForField = new ArrayList<String>();

                            return new FieldVisitor(API_VERSION) {
                                @Override
                                public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
                                    annotationsForField.add(descriptor);
                                    return null;  // TODO robust enough?
                                }

                                @Override
                                public void visitEnd() {
                                    for (AnnotatedFieldVisitor handler : filterHandlers(AnnotatedFieldVisitor.class, handlers).collect(toList())) {
                                        handler.finishedVisitingField(className, testType, access, name, descriptor, signature, annotationsForField);
                                    }
                                }
                            };
                        }

                        @Override
                        public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {

                            final Map<String, List<Object>> annotationValues = new LinkedHashMap<>();

                            return new AnnotationVisitor(API_VERSION) {
                                @Override
                                public void visit(final String annotationName, final Object value) {
                                    for (ClassAnnotationVisitor handler : filterHandlers(ClassAnnotationVisitor.class, handlers).collect(toList())) {
                                        handler.visitClassAnnotation(className, descriptor, annotationName, value);
                                    }
                                }

                                @Override
                                public AnnotationVisitor visitArray(final String arrayName) {

                                    return new AnnotationVisitor(API_VERSION) {
                                        @Override
                                        public void visit(final String name, final Object value) {
                                            if (!annotationValues.containsKey(arrayName)) {
                                                annotationValues.put(arrayName, new ArrayList<>());
                                            }
                                            annotationValues.get(arrayName).add(value);
                                        }

                                        @Override
                                        public void visitEnum(final String name, final String descriptor, final String value) {
                                            if (!annotationValues.containsKey(arrayName)) {
                                                annotationValues.put(arrayName, new ArrayList<>());
                                            }
                                            annotationValues.get(arrayName).add(stripEnd(descriptor, ";") + "." + value);
                                        }
                                    };
                                }

                                @Override
                                public void visitEnd() {
                                    for (ClassAnnotationVisitor handler : filterHandlers(ClassAnnotationVisitor.class, handlers).collect(toList())) {
                                        handler.visitClassAnnotation(className, descriptor, annotationValues);
                                    }
                                }
                            };
                        }

                        @Override
                        public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {

                            for (MethodDefinitionVisitor handler : filterHandlers(MethodDefinitionVisitor.class, handlers).collect(toList())) {
                                handler.visitMethod(access, methodName, desc, signature, exceptions);
                            }

                            var ldcs = new ArrayList<String>();

                            return new MethodVisitor(API_VERSION) {

                                String lastAnnotationDescriptor = null;

                                @Override
                                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                                    for (MethodCallVisitor handler : filterHandlers(MethodCallVisitor.class, handlers).collect(toList())) {
                                        handler.visitMethodCall(className, owner, name, desc);
                                    }

                                    for (VirtualMethodWithParamsVisitor handler : filterHandlers(VirtualMethodWithParamsVisitor.class, handlers).collect(toList())) {
                                        handler.visit(eachClass, methodName, owner, name, desc, ldcs);
                                    }
                                }

                                @Override
                                public void visitLdcInsn(final Object param) {
                                    if (param != null) {
                                        ldcs.add(param.toString());
                                    }
                                }

                                @Override
                                public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
                                    lastAnnotationDescriptor = descriptor;

                                    for (MethodAnnotationVisitor handler : filterHandlers(MethodAnnotationVisitor.class, handlers).collect(toList())) {
                                        handler.visitMethodAnnotation(className, descriptor, methodName, null);
                                    }

                                    return null;
                                }
                            };
                        }
                    }, 0);
                }

                if (doneCheck.done()) {
                    break;
                }
            }
        }
    }

    private static <T> Stream<T> filterHandlers(Class<T> clazz, AsmVisitor... visitors) {
        return Arrays.stream(visitors).filter(clazz::isInstance).map(clazz::cast);
    }
}
