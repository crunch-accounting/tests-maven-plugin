package uk.co.crunch.platform.utils;

import org.objectweb.asm.*;
import uk.co.crunch.platform.asm.*;
import uk.co.crunch.platform.asm.AsmVisitor.DoneCheck;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

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

                    final String className = eachClass.getFileName().toString();

                    new ClassReader(theStream).accept(new ClassVisitor(API_VERSION) {

                        @Override
                        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                            for (ClassDefinitionVisitor handler : filterHandlers(ClassDefinitionVisitor.class, handlers).collect(toList())) {
                                handler.visitClass(access, name, signature, superName, interfaces);
                            }
                        }

                        @Override
                        public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {

                            final Map<String, List<Object>> annotationValues = new LinkedHashMap<>();
                            final String[] currentArrayName = {""};

                            return new AnnotationVisitor(API_VERSION) {
                                @Override
                                public void visit(final String annotationName, final Object value) {
                                    for (ClassAnnotationVisitor handler : filterHandlers(ClassAnnotationVisitor.class, handlers).collect(toList())) {
                                        handler.visitClassAnnotation(className, descriptor, annotationName, value);
                                    }
                                }

                                @Override
                                public AnnotationVisitor visitArray(final String arrayName) {

                                    currentArrayName[0] = arrayName;

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

                            return new MethodVisitor(API_VERSION) {

                                String lastAnnotationDescriptor = null;

                                @Override
                                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                                    for (MethodCallVisitor handler : filterHandlers(MethodCallVisitor.class, handlers).collect(toList())) {
                                        handler.visitMethodCall(className, owner, name, desc);
                                    }
                                }

                                @Override
                                public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
                                    lastAnnotationDescriptor = descriptor;

                                    for (MethodAnnotationVisitor handler : filterHandlers(MethodAnnotationVisitor.class, handlers).collect(toList())) {
                                        handler.visitMethodAnnotation(className, descriptor, methodName, null);
                                    }

                                    return new AnnotationVisitor(API_VERSION) {
                                        @Override
                                        public void visit(final String annotationName, final Object value) {
                                            for (MethodAnnotationVisitor handler : filterHandlers(MethodAnnotationVisitor.class, handlers).collect(toList())) {
                                                handler.visitMethodAnnotation(className, lastAnnotationDescriptor, annotationName, value);
                                            }
                                        }

                                        @Override
                                        public AnnotationVisitor visitArray(final String arrayName) {
                                            return new AnnotationVisitor(API_VERSION) {
                                                @Override
                                                public void visit(final String name, final Object value) {
                                                    for (MethodAnnotationVisitor handler : filterHandlers(MethodAnnotationVisitor.class, handlers).collect(toList())) {
                                                        handler.visitMethodAnnotation(className, lastAnnotationDescriptor, arrayName, value);
                                                    }
                                                }
                                            };
                                        }
                                    };
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
