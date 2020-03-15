package uk.co.crunch.forbiddenMethods;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class ForbiddenMethodsUsingTest {

    @Test
    public void newArrayList() {
        var coll = Lists.newArrayList("a", "b");
        assertEquals("Hi", "Hi");
    }
}
