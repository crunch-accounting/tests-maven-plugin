package uk.co.crunch.samples.junit5PublicClass;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class JUnit5PublicAssert {

    @Test
    void testA() {
        assertEquals("Hi", "Hi");
    }
}
