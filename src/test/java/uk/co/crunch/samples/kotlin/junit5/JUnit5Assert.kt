package uk.co.crunch.samples.kotlin.junit5

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class KotlinJUnit5Assert {
    @Test
    fun testA() {
        Assertions.assertEquals("Hi", "Hi")
    }
}
