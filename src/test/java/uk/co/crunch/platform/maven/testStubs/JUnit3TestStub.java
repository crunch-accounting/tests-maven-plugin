package uk.co.crunch.platform.maven.testStubs;

import uk.co.crunch.platform.maven.NonNullableMavenProjectStub;

import java.util.List;

import static java.util.List.of;

public class JUnit3TestStub extends NonNullableMavenProjectStub {

    public JUnit3TestStub() {
        setArtifactId("test-service-" + System.nanoTime());
    }

    @Override
    public List<String> getTestClasspathElements() {
        return of(getClass().getClassLoader().getResource("uk/co/crunch/samples/junit3").getFile());
    }
}
