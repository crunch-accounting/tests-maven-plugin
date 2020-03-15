package uk.co.crunch.platform.handlers;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import uk.co.crunch.platform.asm.VirtualMethodWithParamsVisitor;
import uk.co.crunch.platform.exceptions.CrunchRuleViolationException;
import uk.co.crunch.platform.maven.CrunchServiceMojo;

// Throw an exception if one of a small list of frowned-upon, redundant, obsolete methods are used
public class ForbiddenMethodsDetector implements HandlerOperation {

    private static final String GUAVA_PKG = "com.google.common.";

    private static final Collection<ForbiddenMethod> FORBIDDEN_METHODS = List.of(
            new ForbiddenMethod("Guava", GUAVA_PKG + "collect", "Lists", "newArrayList"),
            new ForbiddenMethod("Guava", GUAVA_PKG + "collect", "Sets", "newHashSet"),
            new ForbiddenMethod("Guava", GUAVA_PKG + "collect", "Sets", "newTreeSet"),
            new ForbiddenMethod("Guava", GUAVA_PKG + "collect", "Maps", "newHashMap"),
            new ForbiddenMethod("dedicated functional", "java.util", "Objects", "nonNull"),
            new ForbiddenMethod("dedicated functional", "java.util", "Objects", "isNull")
    );

    @Override
    public void run(final CrunchServiceMojo mojo) {
        if (!mojo.isDetectForbiddenMethods()) {
            mojo.getLog().warn("Skipping validation of forbidden test methods");
            return;
        }

        var visitor = new BannedMethodVisitor(mojo.getLog());
        mojo.analyseCrunchClasses(() -> false, visitor);
    }

    private static class BannedMethodVisitor implements VirtualMethodWithParamsVisitor {

        private final Log log;

        public BannedMethodVisitor(Log log) {
            this.log = log;
        }

        @Override
        public void visit(Path eachClass, String discoveredMethodName, String owner, String name, String desc, ArrayList<String> ldcs) {
            // Whitelist our own code and some fundamentals: java.lang, Spring's, Kotlin internal, and Wire
            if (owner.startsWith("uk/co/crunch") || owner.startsWith("java/lang") ||
                    owner.startsWith("org/springframework") || owner.startsWith("kotlin/") ||
                    owner.startsWith("com/squareup/wire/")) {
                return;
            }

            for (var each : FORBIDDEN_METHODS) {
                each.test(this.log, eachClass, discoveredMethodName, owner, name);
            }
        }
    }

    private static class ForbiddenMethod {
        private final String message;
        private final String fqClassName;
        private final String className;
        private final String methodName;

        public ForbiddenMethod(String message, String packageName, String className, String methodName) {
            this.message = message;
            this.fqClassName = packageName.replace('.', '/') + "/" + className;
            this.className = className;
            this.methodName = methodName;
        }

        public void test(Log log, Path eachClass, String discoveredMethodName, String owner, String name) {
            if (owner.equals(this.fqClassName) && name.equals(this.methodName)) {
                throw new CrunchRuleViolationException("Found banned " + this.message + " " + this.className + "." + this.methodName + "() usage in " + getFqn(eachClass, discoveredMethodName) + "()");
            }
        }

        private static String getFqn(Path eachClass, String methodName) {
            return StringUtils.substringBeforeLast(eachClass.getFileName().toString(), ".class") + "." + methodName;
        }
    }
}
