package uk.co.crunch.samplesDubious.junit5Dubious;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class DubiousAnnotationsUnitTest {

    @Mock private Object mock1;
    @Spy private Logger log;
    private State state = new State();

    private final static String CONST = "";

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
    }

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
