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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class TestHandlerTest {

    @Spy
    Log logger;

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
    public void testJUnit3() throws Exception {
        try {
            runConfig("JUnit3TestPom");
            fail();
        } catch (CrunchRuleViolationException e) {
            assertThat(e.getMessage()).isEqualTo("CrunchRuleViolationException: We should stop using JUnit3 assertions (JUnit3Assert.assertEquals)");
        }

        verify(this.logger).info("Test analysis [Java] completed in 0 msecs");
        verify(this.logger).info("Assertion types in use: [JUnit3]");
        verifyNoMoreInteractions(this.logger);
    }

    @Test
    public void testJUnit4AssertionsAndTest() throws Exception {
        try {
            runConfig("JUnit4TestPom");
            fail();
        } catch (CrunchRuleViolationException e) {
            assertThat(e.getMessage()).isEqualTo("CrunchRuleViolationException: We should stop using JUnit4 assertions (JUnit4AssertAndTestUnitTest.assertEquals)");
        }

        verify(this.logger).info("Test analysis [Java] completed in 0 msecs");
        verify(this.logger).info("Assertion types in use: [JUnit4]");
        verifyNoMoreInteractions(this.logger);
    }

    @Test
    public void testHamcrest() throws Exception {
        try {
            runConfig("HamcrestTestPom");
            fail();
        } catch (CrunchRuleViolationException e) {
            assertThat(e.getMessage()).isEqualTo("CrunchRuleViolationException: We should stop using Hamcrest (HamcrestAssert.assertThat)");
        }

        verify(this.logger).info("Test analysis [Java] completed in 0 msecs");
        verify(this.logger).info("Assertion types in use: [Hamcrest]");
        verifyNoMoreInteractions(this.logger);
    }

    @Test
    public void testTruthAssertions() throws Exception {
        try {
            runConfig("TruthTestPom");
            fail();
        } catch (CrunchRuleViolationException e) {
            assertThat(e.getMessage()).isEqualTo("CrunchRuleViolationException: We should stop using Google Truth assertions (TruthAssert.assertThat)");
        }

        verify(this.logger).info("Test analysis [Java] completed in 0 msecs");
        verify(this.logger).info("Assertion types in use: [Truth]");
        verifyNoMoreInteractions(this.logger);
    }

    @Test
    public void testJUnit5() throws Exception {
        try {
            runConfig("JUnit5TestPom");
            fail();
        } catch (CrunchRuleViolationException e) {
            assertThat(e.getMessage()).isEqualTo("CrunchRuleViolationException: We should stop using JUnit5 assertions (JUnit5Assert.assertEquals)");
        }

        verify(this.logger).info("Test analysis [Java] completed in 0 msecs");
        verify(this.logger).info("Assertion types in use: [JUnit5]");
        verifyNoMoreInteractions(this.logger);
    }

    @Test
    public void testKotlinJUnit5() throws Exception {
        try {
            runConfig("KotlinJUnit5TestPom");
            fail();
        } catch (CrunchRuleViolationException e) {
            assertThat(e.getMessage()).isEqualTo("CrunchRuleViolationException: We should stop using JUnit5 assertions (KotlinJUnit5Assert.assertEquals)");
        }

        verify(this.logger).info("Test analysis [Kotlin] completed in 0 msecs");
        verify(this.logger).info("Assertion types in use: [JUnit5]");
        verifyNoMoreInteractions(this.logger);
    }

    @Test
    public void testAssertJ() throws Exception {
        runConfig("AssertJTestPom");

        verify(this.logger).info("Test analysis [Java] completed in 0 msecs");
        verify(this.logger).info("Assertion types in use: [AssertJ]");
        verifyNoMoreInteractions(this.logger);
    }

    @Test
    public void testKotlinStrikt() throws Exception {
        runConfig("KotlinStriktTestPom");

        verify(this.logger).info("Test analysis [Kotlin] completed in 0 msecs");
        verify(this.logger).info("Assertion types in use: [Strikt]");
        verifyNoMoreInteractions(this.logger);
    }

    @Test
    public void testJUnit5PublicClass() throws Exception {
        try {
            runConfig("JUnit5PublicClassTestPom");
            fail();
        } catch (CrunchRuleViolationException e) {
            assertThat(e.getMessage()).isEqualTo("CrunchRuleViolationException: We should stop using JUnit5 assertions (JUnit5PublicAssert.assertEquals)");
        }

        verify(this.logger).warn("Java test class `JUnit5PublicAssert` does not need to be public");
        verify(this.logger).info("Test analysis [Java] completed in 0 msecs");
        verify(this.logger).info("Assertion types in use: [JUnit5]");
        verifyNoMoreInteractions(this.logger);
    }

    @Test
    public void testJUnit5PublicMethodWithMatchingOverride() throws Exception {
        runConfig("JUnit5PublicMethodTestPom");

        verify(this.logger).info("JUnit5 assertThrows() can be replaced by AssertJ too: https://www.baeldung.com/assertj-exception-assertion");
        verify(this.logger).warn("We should stop using JUnit5 assertions (JUnit5PublicMethodAssertUnitTest.assertEquals)");
        verify(this.logger).warn("Unit test class `JUnit5PublicMethodAssertUnitTest`, methods: [testA] do not need to be public");
        verify(this.logger).warn("Unit test class `JUnit5PublicMethodAssertUnitTest`, methods: [testA] do not need the prefix 'test'");
        verify(this.logger).info("Test analysis [Java] completed in 0 msecs");
        verify(this.logger).info("Assertion types in use: [JUnit5 x 3]");
        verifyNoMoreInteractions(this.logger);
    }

    @Test
    public void testJUnit5DubiousAnnotations() throws Exception {
        runConfigWithAllOverrides("JUnit5DubiousAnnotationsTestPom");

        verify(this.logger).warn("Unit test class `DubiousAnnotationsUnitTest`, fields: [state] are dubious");
        verify(this.logger).warn("Integration test class `DubiousAnnotationsIntegrationTest`, fields: [rogueMock, rogueSpy, state] are dubious");
        verify(this.logger).warn("Unclassified test class `uk.co.crunch.samplesDubious.junit5Dubious.DubiousAnnotationsMiscellaneousTest` should clarify whether it is a Unit or Integration test");
        verify(this.logger).info("Can't validate fields for unclear test type");
        verify(this.logger).info("Test analysis [Java x 3] completed in 0 msecs");
        verify(this.logger).info("Assertion types in use: [AssertJ x 3]");
        verifyNoMoreInteractions(this.logger);
    }

    @Test
    public void testAll() throws Exception {
        runConfigWithAllOverrides("AllTestsPom");

        verify(this.logger).warn("Cannot combine JUnit 4 and JUnit 5 tests! See: JUnit4AssertAndTestUnitTest.class");

        verify(this.logger).warn("We should stop using Hamcrest (HamcrestAssert.assertThat)");
        verify(this.logger).warn("We should stop using Google Truth assertions (TruthAssert.assertThat)");
        verify(this.logger).warn("We should stop using JUnit3 assertions (JUnit3Assert.assertEquals)");
        verify(this.logger).warn("We should stop using JUnit4 assertions (JUnit4AssertAndTestUnitTest.assertEquals)");
        verify(this.logger).warn("We should stop using JUnit5 assertions (JUnit5Assert.assertEquals)");

        verify(this.logger).warn("Mixed test class `HamcrestAssert`, methods: [testA] do not need the prefix 'test'");
        verify(this.logger).warn("Mixed test class `JUnit3Assert`, methods: [testA] do not need the prefix 'test'");
        verify(this.logger).warn("Mixed test class `JUnit5Assert`, methods: [testA] do not need the prefix 'test'");
        verify(this.logger).warn("Mixed test class `JUnit5PublicAssert`, methods: [testA] do not need the prefix 'test'");
        verify(this.logger).warn("Unit test class `JUnit5PublicMethodAssertUnitTest`, methods: [testA] do not need to be public");
        verify(this.logger).warn("Unit test class `JUnit5PublicMethodAssertUnitTest`, methods: [testA] do not need the prefix 'test'");
        verify(this.logger).warn("Mixed test class `KotlinJUnit5Assert`, methods: [testA] do not need the prefix 'test'");
        verify(this.logger).warn("Java test class `JUnit5PublicAssert` does not need to be public");

        verify(this.logger).info("JUnit5 assertThrows() can be replaced by AssertJ too: https://www.baeldung.com/assertj-exception-assertion");
        verify(this.logger).warn("Kotlin tests ought to start moving from AssertJ => Strikt, where possible");

        verify(this.logger).info("Test analysis [Java x 8, Kotlin x 2] completed in 0 msecs");
        verify(this.logger).info("Assertion types in use: [Hamcrest, AssertJ, JUnit3, JUnit4, JUnit5 x 6, Strikt, Truth]");
        verifyNoMoreInteractions(this.logger);
    }

    private void runConfig(String configName) throws Exception {
        mojoForPom(configName).execute(List.of(new TestHandler(this.logger, () -> 0L, false)));
    }

    private void runConfigWithAllOverrides(String configName) throws Exception {
        mojoForPom(configName).execute(List.of(new TestHandler(this.logger, () -> 0L, true)));
    }

    @SuppressWarnings("ConstantConditions")
    private CrunchServiceMojo mojoForPom(String configName) throws Exception {
        final File pomFile = new File(getClass().getClassLoader().getResource(configName + ".xml").getFile());
        return (CrunchServiceMojo) this.rule.lookupMojo("generate", pomFile);
    }
}
