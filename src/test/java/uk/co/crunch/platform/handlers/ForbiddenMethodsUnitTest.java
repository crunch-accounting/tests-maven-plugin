package uk.co.crunch.platform.handlers;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import uk.co.crunch.platform.exceptions.CrunchRuleViolationException;
import uk.co.crunch.platform.maven.CrunchServiceMojo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ForbiddenMethodsUnitTest {
    @Spy Log logger;

    @Rule
    public final MojoRule rule = new MojoRule() {
        @Override
        protected void before() {
        }

        @Override
        protected void after() {
        }
    };

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testBasic() throws Exception {
        try {
            runConfig("ForbiddenMethodsUnitTestPom");
            fail();
        } catch (CrunchRuleViolationException e) {
            assertThat(e.getMessage()).isEqualTo("CrunchRuleViolationException: Found banned Guava Lists.newArrayList() usage in ForbiddenMethodsUsingTest.newArrayList()");
        }

        verifyNoMoreInteractions(this.logger);
    }

    private void runConfig(String configName) throws Exception {
        mojoForPom(configName).execute(List.of(new ForbiddenMethodsDetector(this.logger)));
    }

    @SuppressWarnings("ConstantConditions")
    private CrunchServiceMojo mojoForPom(String configName) throws Exception {
        var pomFile = new File(getClass().getClassLoader().getResource(configName + ".xml").getFile());
        return (CrunchServiceMojo) this.rule.lookupMojo("generate", pomFile);
    }
}
