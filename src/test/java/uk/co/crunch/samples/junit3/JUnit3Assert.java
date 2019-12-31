package uk.co.crunch.samples.junit3;

import org.junit.jupiter.api.Test;

import static junit.framework.TestCase.assertEquals;

class JUnit3Assert {

    @Test
    void testA() {
        assertEquals("Hi", "Hi");
    }
}
