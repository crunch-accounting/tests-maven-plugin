package uk.co.crunch.platform.handlers;

import uk.co.crunch.platform.maven.CrunchServiceMojo;

public class TestHandler implements HandlerOperation {
    @Override
    public void run(CrunchServiceMojo mojo) {
        System.out.println(mojo.getProject().getTestCompileSourceRoots());
        System.out.println(mojo.getTestClasspathElementsList());
    }
}
