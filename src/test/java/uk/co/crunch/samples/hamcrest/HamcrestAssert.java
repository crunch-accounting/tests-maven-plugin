package uk.co.crunch.samples.hamcrest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;


class HamcrestAssert {

    @Test
    void testA() {
        assertThat("Hi", equalTo("Hi"));
    }
}
