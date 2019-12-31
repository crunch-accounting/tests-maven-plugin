package uk.co.crunch.platform.utils;

import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import uk.co.crunch.platform.exceptions.CrunchRuleViolationException;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SpringBootUtils {

    public static String getContextPathProperty(final Properties appProps) {
        final String first = appProps.getProperty("server.context-path");
        if (!isNullOrEmpty(first)) {
            return first;
        }

        final String retry = appProps.getProperty("server.contextPath");
        if (!isNullOrEmpty(retry)) {
            return retry;
        }

        final String springBoot2Prop = appProps.getProperty("server.servlet.context-path");
        if (!isNullOrEmpty(springBoot2Prop)) {
            return springBoot2Prop;
        }

        throw new RuntimeException("Could not find a server context-path in application.properties");
    }

    private static String getContextPathProperty(final Map<String, Map<String, String>> yamlProperties) {
        // FIXME Pretty crude - assumes nested properties unlike Spring's parser
        // Since 2.7.12, this is for Spring Boot 2.x only
        final String rawValue = yamlProperties.get("server").get("servlet.context-path");
        if (!isNullOrEmpty(rawValue)) {
            return rawValue;
        }

        throw new RuntimeException("Could not find a server context-path in application.y?ml");
    }

    public static String getSpringBootVersion(final MavenProject project) {
        for (Artifact each : project.getArtifacts()) {
            if (each.getGroupId().equals("org.springframework.boot") &&
                each.getArtifactId().equals("spring-boot") &&
                !each.getScope().equals("test")) {
                return each.getVersion();
            }
        }
        throw new CrunchRuleViolationException("Could not detect Spring Boot version");
    }

    public static String getWebServerName(final MavenProject project) {
        for (Artifact each : project.getArtifacts()) {
            if (!each.getGroupId().equals("org.springframework.boot")) {
                continue;
            }

            if (each.getArtifactId().equals("spring-boot-starter-tomcat")) {
                return "Tomcat";
            }

            if (each.getArtifactId().equals("spring-boot-starter-undertow")) {
                return "Undertow";
            }

            if (each.getArtifactId().equals("spring-boot-starter-jetty")) {
                return "Jetty";
            }
        }
        return "n/a";
    }
}
