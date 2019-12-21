package uk.co.crunch.platform.handlers;

import uk.co.crunch.platform.maven.CrunchServiceMojo;

@FunctionalInterface
public interface HandlerOperation {
    void run(CrunchServiceMojo mojo);
}
