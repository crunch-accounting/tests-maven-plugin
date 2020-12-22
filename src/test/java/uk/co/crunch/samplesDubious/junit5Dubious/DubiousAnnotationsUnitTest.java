package uk.co.crunch.samplesDubious.junit5Dubious;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DubiousAnnotationsUnitTest {

    @Mock private Object mock1;
    @Spy private Logger log;
    private final State state = new State();

    private final static String CONST = "";

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
