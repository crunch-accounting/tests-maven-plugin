package uk.co.crunch.platform.maven.testStubs;

import java.util.List;

import uk.co.crunch.platform.maven.NonNullableMavenProjectStub;

import static java.util.List.of;

@SuppressWarnings("ConstantConditions")
public class JUnit5TestStub extends NonNullableMavenProjectStub {

    public JUnit5TestStub() {
        setArtifactId("test-service-" + System.nanoTime());
    }

    @Override
    public List<String> getTestClasspathElements() {
        return of(getClass().getClassLoader().getResource("uk/co/crunch/samples/junit5").getFile());
    }
}
