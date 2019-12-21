package uk.co.crunch.platform.maven;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CaseFormat;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.jtwig.JtwigModel;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import uk.co.crunch.platform.asm.AsmVisitor;
import uk.co.crunch.platform.asm.AsmVisitor.DoneCheck;
import uk.co.crunch.platform.handlers.HandlerOperation;
import uk.co.crunch.platform.handlers.TestHandler;
import uk.co.crunch.platform.utils.AsmUtils;
import uk.co.crunch.platform.utils.FileUtils;
import uk.co.crunch.platform.utils.MojoUtils;
import uk.co.crunch.platform.utils.SpringBootUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;

import static com.google.common.base.Strings.isNullOrEmpty;

@Mojo(
        name = "generate",
        defaultPhase = LifecyclePhase.TEST_COMPILE,
        requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM,
        threadSafe = true)
public class CrunchServiceMojo
        extends AbstractMojo {

    /**
     * The character encoding scheme to be applied when filtering resources.
     */
    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    protected String encoding;

    /**
     *
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter
    private boolean skipLogbackGeneration;  // default = null => false

    @Parameter
    private boolean skipReadmeGeneration;  // default = null => false

    @Parameter
    private boolean skipLocustGeneration;  // default = null => false

    @Parameter
    private boolean skipKronGeneration;  // default = null => false

    @Parameter
    private boolean skipSwaggerGeneration;  // default = null => false

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    @Component(role = MavenResourcesFiltering.class, hint = "default")
    protected MavenResourcesFiltering mavenResourcesFiltering;

    private final JtwigModel model = JtwigModel.newModel();
    private final MojoState state = new MojoState();

    private URLClassLoader testClassLoader;  // cached
    private List<String> testClasspathElements;  // cached
    private Reflections reflectionsForAnnotations;  // cached
    private String acquiredApplicationName;  // cached

    private final JtwigModel sharedServiceMetadataModel = JtwigModel.newModel();

    public List<HandlerOperation> defaultHandlers() {
        final List<HandlerOperation> operations = new ArrayList<>();

        operations.add(new TestHandler());

        return operations;
    }

    public void execute() {
        execute(defaultHandlers());
    }

    public void execute(List<HandlerOperation> operations) {

        model.with("serviceName", getServiceName())
                .with("serviceNameCamel", getServiceNameUpperCamel());

        state.setDataModel(model);

        this.getMetadataModel().with("bootstrap_content", "n/a")
                .with("mysql_schema", "n/a")
                .with("mysql_diagram_links", "`n/a`")
                .with("rabbit_permissions", "n/a");

        operations.forEach(each -> each.run(this));
    }

    private String getServiceNameUpperCase() {
        return project.getArtifactId().toUpperCase().replace('-', '_');  // Normalise
    }

    public String getServiceName() {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, getServiceNameUpperCase());
    }

    private String getServiceNameUpperCamel() {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, getServiceNameUpperCase());
    }

    public String getApplicationName() {
        if (acquiredApplicationName == null) {
            // Bit iffy, but seems to work fine
            final File mainResourcesBaseDir = MojoUtils.pickResourcesDirectory(project.getResources());

            getLog().info("Looking for application name in: " + FileUtils.getFriendlyFileName(mainResourcesBaseDir));

            try {
                try (InputStream is = new FileInputStream(new File(mainResourcesBaseDir, "application.properties"))) {
                    final Properties appProps = new Properties();
                    appProps.load(is);

                    // Must trust this, if set
                    acquiredApplicationName = appProps.getProperty("spring.application.name");

                    if (isNullOrEmpty(acquiredApplicationName)) {
                        acquiredApplicationName = SpringBootUtils.getContextPathProperty(appProps).replace("/", "").replace("-service", "");
                    }
                }
            } catch (FileNotFoundException e) {
                // Should really fail compile, given that only the Config Server is really affected!
                getLog().error("Bootstrap Generation: could not find an application.properties (application.y?ml not yet supported)");
                return acquiredApplicationName;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return acquiredApplicationName;
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

    public URLClassLoader getTestClassLoader() {
        if (testClassLoader != null) {
            return testClassLoader;
        }

        final List<String> runtimeClasspathElements = getTestClasspathElementsList();
        final URL[] runtimeUrls = new URL[runtimeClasspathElements.size()];
        int i = 0;

        for (String element : runtimeClasspathElements) {
            try {
                runtimeUrls[i++] = new File(element).toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);  // Not likely
            }
        }

        testClassLoader = new URLClassLoader(runtimeUrls, Thread.currentThread().getContextClassLoader());
        return testClassLoader;
    }

    public Reflections getReflectionsForAnnotations() {
        if (reflectionsForAnnotations == null) {
            reflectionsForAnnotations = new Reflections("uk.co.crunch", getTestClassLoader(),
                    new SubTypesScanner(),
                    new TypeAnnotationsScanner(),
                    new MethodAnnotationsScanner());
        }
        return reflectionsForAnnotations;
    }

    public boolean hasClass(final String fqn) {
        try {
            Class.forName(fqn, false, getTestClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean hasArtifact(final Predicate<Artifact> rule) {
        for (Artifact each : this.project.getArtifacts()) {
            if (rule.test(each)) {
                return true;
            }
        }
        return false;
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

    @VisibleForTesting
    public MojoState getState() {
        return state;
    }

    public JtwigModel getModel() {
        return model;
    }

    public MavenProject getProject() {
        return project;
    }

    public MavenResourcesFiltering getMavenResourcesFiltering() {
        return mavenResourcesFiltering;
    }

    public JtwigModel getMetadataModel() {
        return sharedServiceMetadataModel;
    }
}
