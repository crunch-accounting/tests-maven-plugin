package uk.co.crunch.samples.truth;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

class TruthAssert {

    @Test
    void truthTest() {
        assertThat("Hi").isEqualTo("Hi");
    }
}
