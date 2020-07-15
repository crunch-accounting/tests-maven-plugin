package uk.co.crunch.platform.maven

import org.yaml.snakeyaml.Yaml
import uk.co.crunch.platform.utils.MojoUtils
import java.io.File
import java.io.IOException
import java.util.*

object PropertiesHelper {

    @JvmStatic
    fun storeProperties(mojo: CrunchServiceMojo) {
        val resDir = MojoUtils.pickResourcesDirectory(mojo.getProject().testResources)
        try {
            File(resDir, "application.properties").inputStream().use {
                val appProps = Properties()
                appProps.load(it)
                mojo.log.info("Storing ${appProps.size} properties from `application.properties`")
                mojo.metadataModel["application_properties"] = appProps
            }
        } catch (e: IOException) {
            try {
                File(resDir, "application.yml").inputStream().use {
                    val yaml = Yaml().loadAs(it, MutableMap::class.java) as Map<*, *>
                    mojo.log.info("Storing ${yaml.size} properties from `application.yml`")
                    mojo.metadataModel["application_properties"] = yaml
                }
            } catch (e: IOException) {
                mojo.log.info("No application properties found")
                mojo.metadataModel["application_properties"] = mapOf<String, Any>()
            }
        }
    }
}
