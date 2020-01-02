package uk.co.crunch.samples.kotlin.strikt

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

internal class KotlinJUnit5Assert {
    @Test
    fun hiTest() {
        expectThat("Hi").isEqualTo("Hi")
    }
}
