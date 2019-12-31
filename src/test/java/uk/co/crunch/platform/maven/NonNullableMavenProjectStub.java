package uk.co.crunch.platform.maven;

import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

import java.util.Collections;
import java.util.List;

public class NonNullableMavenProjectStub extends MavenProjectStub {

    @Override
    public List<String> getRuntimeClasspathElements() {
        return Collections.emptyList();
    }
}
