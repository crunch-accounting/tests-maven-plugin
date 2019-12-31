package uk.co.crunch.platform.maven

import org.apache.maven.plugin.testing.stubs.MavenProjectStub

open class NonNullableMavenProjectStub : MavenProjectStub() {
    override fun getRuntimeClasspathElements() = listOf<String>()
}
