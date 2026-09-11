package com.davils.example

import org.junit.jupiter.api.Test

/**
 * Integration tests for [Greeter].
 *
 * Lives in the integration suite to show what that suite is for: a test that depends on
 * something outside the process. Here it is one environment variable, set on the suite rather
 * than on every test task in the project.
 *
 * @since 1.0.0
 */
class GreeterIntegrationTest {
    /**
     * Verifies that the suite's own environment reaches the test JVM.
     *
     * @since 1.0.0
     */
    @Test
    fun readsItsOwnEnvironment() {
        assert(System.getenv("EXAMPLE_INTEGRATION") == "true")
    }
}
