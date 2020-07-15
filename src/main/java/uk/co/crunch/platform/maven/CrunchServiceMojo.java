package uk.co.crunch.platform.maven;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import uk.co.crunch.platform.asm.AsmVisitor;
import uk.co.crunch.platform.asm.AsmVisitor.DoneCheck;
import uk.co.crunch.platform.handlers.ForbiddenMethodsDetector;
import uk.co.crunch.platform.handlers.HandlerOperation;
import uk.co.crunch.platform.handlers.InfrastructureEnabledInTestDetector;
import uk.co.crunch.platform.handlers.TestHandler;
import uk.co.crunch.platform.template.DataModel;
import uk.co.crunch.platform.utils.AsmUtils;

@Mojo(
    name = "generate",
    defaultPhase = LifecyclePhase.TEST_COMPILE,
    requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM,
    threadSafe = true)
public class CrunchServiceMojo
    extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter
    private boolean detectForbiddenMethods = true;

    private List<String> testClasspathElements;  // cached

    private final DataModel sharedServiceMetadataModel = new DataModel();

    public List<HandlerOperation> defaultHandlers() {
        final List<HandlerOperation> operations = new ArrayList<>();

        operations.add(new InfrastructureEnabledInTestDetector(this.getLog()));
        operations.add(new ForbiddenMethodsDetector(this.getLog()));
        operations.add(new TestHandler(this.getLog(), System::currentTimeMillis, false));

        return operations;
    }

    public void execute() {
        execute(defaultHandlers());
    }

    public void execute(List<HandlerOperation> operations) {
        PropertiesHelper.storeProperties(this);

        operations.forEach(each -> each.run(this));
    }

    public List<String> getTestClasspathElementsList() {
        if (testClasspathElements == null) {
            try {
                this.testClasspathElements = project.getTestClasspathElements();
            } catch (DependencyResolutionRequiredException e) {
                throw new RuntimeException(e);
            }
        }
        return testClasspathElements;
    }

    public void analyseCrunchClasses(final DoneCheck doneCheck, final AsmVisitor... handlers) {
        for (String classPathEntry : getTestClasspathElementsList()) {
            try {
                AsmUtils.visitCrunchClasses(classPathEntry, doneCheck, handlers);

                if (doneCheck.done()) {
                    break;
                }
            } catch (IOException e) {
                // Just ignore
            }
        }
    }

    public MavenProject getProject() {
        return project;
    }

    public boolean isDetectForbiddenMethods() {
        return detectForbiddenMethods;
    }

    public DataModel getMetadataModel() {
        return sharedServiceMetadataModel;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getApplicationProperties() {
        return (Map<String, Object>) sharedServiceMetadataModel.get("application_properties");
    }
}
