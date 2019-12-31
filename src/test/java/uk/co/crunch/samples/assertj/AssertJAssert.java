package uk.co.crunch.samples.assertj;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class AssertJAssert {

    @Test
    void assertJTest() {
        assertThat("Hi").isEqualTo("Hi");
    }
}
