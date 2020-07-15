package uk.co.crunch.platform.handlers

import org.apache.maven.plugin.logging.Log
import uk.co.crunch.platform.exceptions.CrunchRuleViolationException
import uk.co.crunch.platform.maven.CrunchServiceMojo

class InfrastructureEnabledInTestDetector(private val logger: Log) : HandlerOperation {
    override fun run(mojo: CrunchServiceMojo) {
        val props = mojo.applicationProperties

        if (parseBoolean(props["spring.sleuth.enabled"])) {
            throw CrunchRuleViolationException("'spring.sleuth.enabled' should be set to false (hierarchical YAML not yet supported)")
        }

        if (parseBoolean(props["spring.cloud.kubernetes.enabled"])) {
            throw CrunchRuleViolationException("'spring.cloud.kubernetes.enabled' should be set to false (hierarchical YAML not yet supported)")
        }
    }

    // treat true as default
    private fun parseBoolean(value: Any?): Boolean = if (value == null) true else java.lang.Boolean.valueOf(value.toString())
}
