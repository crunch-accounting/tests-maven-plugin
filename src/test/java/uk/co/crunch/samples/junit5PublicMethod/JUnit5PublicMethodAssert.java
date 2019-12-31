package uk.co.crunch.samples.junit5PublicMethod;

import org.junit.jupiter.api.Test;
import uk.co.crunch.platform.api.tests.CrunchTestValidationOverride;
import uk.co.crunch.platform.api.tests.CrunchTestValidationOverrides;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@CrunchTestValidationOverride(CrunchTestValidationOverrides.JUNIT5_ASSERTIONS)
class JUnit5PublicMethodAssert {

    @Test
    public void testA() {
        assertEquals("Hi", "Hi");
        assertThrows(RuntimeException.class, this::doSomething);
    }

    private void doSomething() {
        throw new RuntimeException();
    }
}
