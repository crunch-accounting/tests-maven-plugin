package uk.co.crunch.samples.junit5;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class JUnit5Assert {

    @Test
    void testA() {
        assertEquals("Hi", "Hi");
    }
}
