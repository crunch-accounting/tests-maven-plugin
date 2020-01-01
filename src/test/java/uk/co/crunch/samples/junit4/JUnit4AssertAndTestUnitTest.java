package uk.co.crunch.samples.junit4;


import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class JUnit4AssertAndTestUnitTest {

    @Test
    public void testA() {
        assertEquals("Hi", "Hi");
    }
}
