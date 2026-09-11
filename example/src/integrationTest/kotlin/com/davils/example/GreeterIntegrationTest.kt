package com.davils.example

import org.junit.jupiter.api.Test

class GreeterIntegrationTest {
    @Test
    fun readsItsOwnEnvironment() {
        assert(System.getenv("EXAMPLE_INTEGRATION") == "true")
    }
}
