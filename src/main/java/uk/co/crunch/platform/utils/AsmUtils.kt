package uk.co.crunch.platform.utils

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import uk.co.crunch.platform.TestType
import uk.co.crunch.platform.asm.AnnotatedFieldVisitor
import uk.co.crunch.platform.asm.AsmVisitor
import uk.co.crunch.platform.asm.AsmVisitor.DoneCheck
import uk.co.crunch.platform.asm.ClassAnnotationVisitor
import uk.co.crunch.platform.asm.ClassDefinitionVisitor
import uk.co.crunch.platform.asm.MethodAnnotationVisitor
import uk.co.crunch.platform.asm.MethodCallVisitor
import uk.co.crunch.platform.asm.MethodDefinitionVisitor
import uk.co.crunch.platform.asm.VirtualMethodWithParamsVisitor
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*

object AsmUtils {
    private const val API_VERSION = Opcodes.ASM9
    private val CRUNCH_PACKAGE_PATH = "uk/co/crunch/".replace("/", File.separator)

    @JvmStatic
    @Throws(IOException::class)
    fun visitCrunchClasses(classPathEntry: String, doneCheck: DoneCheck, handlers: List<AsmVisitor>) {
        val classesDir = File(classPathEntry)
        if (!classesDir.isDirectory) {
            return
        }
        Files.walk(classesDir.toPath()).use { classPathStream ->
            for (eachClass in classPathStream.filter { it.toString().endsWith(".class") && it.toString().contains(CRUNCH_PACKAGE_PATH) }
                .sorted() // Seems to be issue with Maven 3.3.x vs Maven 3.5
            ) {
                Files.newInputStream(eachClass).use { theStream ->
                    val className = eachClass.fileName.toString()
                    val testType =
                        if (className.endsWith("IntegrationTest.class")) TestType.Integration else if (className.endsWith("UnitTest.class")) TestType.Unit else TestType.Mixed

                    ClassReader(theStream).accept(object : ClassVisitor(API_VERSION) {
                        override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String, interfaces: Array<String>) {
                            filterHandlers<ClassDefinitionVisitor>(handlers).forEach {
                                it.visitClass(access, name, signature, superName, interfaces)
                            }
                        }

                        override fun visitEnd() {
                            filterHandlers<ClassDefinitionVisitor>(handlers).forEach {
                                it.finishedVisitingClass(className, testType)
                            }
                        }

                        override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): FieldVisitor {
                            // TODO Ensure this is actually a test class, e.g. not a helper / Utils
                            val annotationsForField = ArrayList<String>()

                            return object : FieldVisitor(API_VERSION) {
                                override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                                    annotationsForField.add(descriptor)
                                    return null // TODO robust enough?
                                }

                                override fun visitEnd() {
                                    filterHandlers<AnnotatedFieldVisitor>(handlers).forEach {
                                        it.finishedVisitingField(className, testType, access, name, descriptor, signature, annotationsForField)
                                    }
                                }
                            }
                        }

                        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
                            val annotationValues: MutableMap<String, MutableList<Any>> = LinkedHashMap()

                            return object : AnnotationVisitor(API_VERSION) {
                                override fun visit(annotationName: String, value: Any) {
                                    filterHandlers<ClassAnnotationVisitor>(handlers).forEach {
                                        it.visitClassAnnotation(className, descriptor, annotationName, value)
                                    }
                                }

                                override fun visitArray(arrayName: String): AnnotationVisitor {
                                    return object : AnnotationVisitor(API_VERSION) {
                                        override fun visit(name: String?, value: Any) {
                                            if (!annotationValues.containsKey(arrayName)) {
                                                annotationValues[arrayName] = ArrayList()
                                            }
                                            annotationValues[arrayName]!!.add(value)
                                        }

                                        override fun visitEnum(name: String?, descriptor: String, value: String) {
                                            if (!annotationValues.containsKey(arrayName)) {
                                                annotationValues[arrayName] = ArrayList()
                                            }
                                            annotationValues[arrayName]!!.add(descriptor.removeSuffix(";") + "." + value)
                                        }
                                    }
                                }

                                override fun visitEnd() {
                                    filterHandlers<ClassAnnotationVisitor>(handlers).forEach {
                                        it.visitClassAnnotation(className, descriptor, annotationValues)
                                    }
                                }
                            }
                        }

                        override fun visitMethod(
                            access: Int,
                            methodName: String, desc: String,
                            signature: String?, exceptions: Array<String>?
                        ): MethodVisitor {
                            filterHandlers<MethodDefinitionVisitor>(handlers).forEach {
                                it.visitMethod(access, methodName, desc, signature, exceptions)
                            }

                            val ldcs = ArrayList<String>()
                            return object : MethodVisitor(API_VERSION) {
                                var lastAnnotationDescriptor: String? = null

                                override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
                                    filterHandlers<MethodCallVisitor>(handlers).forEach {
                                        it.visitMethodCall(className, owner, name, desc)
                                    }
                                    filterHandlers<VirtualMethodWithParamsVisitor>(handlers).forEach {
                                        it.visit(eachClass, methodName, owner, name, desc, ldcs)
                                    }
                                }

                                override fun visitLdcInsn(param: Any?) {
                                    param?.let {
                                        ldcs.add(param.toString())
                                    }
                                }

                                override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                                    lastAnnotationDescriptor = descriptor

                                    filterHandlers<MethodAnnotationVisitor>(handlers).forEach {
                                        it.visitMethodAnnotation(className, descriptor, methodName, null)
                                    }
                                    return null
                                }
                            }
                        }
                    }, 0)
                }

                if (doneCheck.done()) {
                    break
                }
            }
        }
    }

    private inline fun <reified T> filterHandlers(visitors: List<AsmVisitor>) = visitors.filterIsInstance(T::class.java)
}
