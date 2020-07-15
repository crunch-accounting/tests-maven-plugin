package uk.co.crunch.platform.utils

import org.apache.maven.model.Resource
import java.io.File

object MojoUtils {
    fun pickResourcesDirectory(resources: List<Resource>) = if (resources.isEmpty()) null else File(resources.first().directory)
}
