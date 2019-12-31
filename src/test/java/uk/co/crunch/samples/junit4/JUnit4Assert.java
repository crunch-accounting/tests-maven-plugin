package uk.co.crunch.samples.junit4;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;


class JUnit4Assert {

    @Test
    void testA() {
        assertEquals("Hi", "Hi");
    }
}
