package uk.co.crunch.samplesDubious.junit5Dubious;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.slf4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.assertj.core.api.Assertions.assertThat;

class DubiousAnnotationsIntegrationTest {

    @Autowired private Object dependency1;
    @Autowired private Logger log;
    @Value("${sss:}") private String injectedValue;

    @MockBean private Object mockBean;
    @SpyBean private Object spyBean;

    @Mock private Object rogueMock;
    @Spy private Object rogueSpy;

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
