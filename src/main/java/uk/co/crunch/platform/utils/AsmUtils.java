package uk.co.crunch.platform.utils;

import org.objectweb.asm.*;
import uk.co.crunch.platform.asm.AsmVisitor;
import uk.co.crunch.platform.asm.AsmVisitor.DoneCheck;
import uk.co.crunch.platform.asm.ClassAnnotationVisitor;
import uk.co.crunch.platform.asm.MethodAnnotationVisitor;
import uk.co.crunch.platform.asm.VirtualMethodVisitor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

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
                        public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
                            for (ClassAnnotationVisitor handler : filterHandlers(ClassAnnotationVisitor.class, handlers).collect(toList())) {
                                handler.visitClassAnnotation(className, descriptor);
                            }
                            return null;
                        }

                        @Override
                        public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
                            return new MethodVisitor(API_VERSION) {

                                String lastAnnotationDescriptor = null;

                                @Override
                                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                                    for (VirtualMethodVisitor handler : filterHandlers(VirtualMethodVisitor.class, handlers).collect(toList())) {
                                        handler.visit(className, owner, name, desc);
                                    }
                                }

                                @Override
                                public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
                                    lastAnnotationDescriptor = descriptor;

                                    for (MethodAnnotationVisitor handler : filterHandlers(MethodAnnotationVisitor.class, handlers).collect(toList())) {
                                        handler.visit(className, descriptor, methodName, null);
                                    }

                                    return new AnnotationVisitor(API_VERSION) {
                                        @Override
                                        public void visit(final String annotationName, final Object value) {
                                            for (MethodAnnotationVisitor handler : filterHandlers(MethodAnnotationVisitor.class, handlers).collect(toList())) {
                                                handler.visit(className, lastAnnotationDescriptor, annotationName, value);
                                            }
                                        }

                                        @Override
                                        public AnnotationVisitor visitArray(final String arrayName) {
                                            return new AnnotationVisitor(API_VERSION) {
                                                @Override
                                                public void visit(final String name, final Object value) {
                                                    for (MethodAnnotationVisitor handler : filterHandlers(MethodAnnotationVisitor.class, handlers).collect(toList())) {
                                                        handler.visit(className, lastAnnotationDescriptor, arrayName, value);
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
