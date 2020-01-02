package uk.co.crunch.samplesDubious.junit5Dubious;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DubiousAnnotationsMiscellaneousTest {

    private final State state = new State();

    @Test
    void hiTest() {
        state.incVal();
        assertThat("Hi").isEqualTo("Hi");
    }

    static class State {
        private int val;

        void incVal() {
            this.val++;
        }
    }
}
