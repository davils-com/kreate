package com.davils.example

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Contract tests for [Greeter].
 *
 * Lives in a suite the project registered itself, and runs only through that suite's tag
 * filter - which is what keeps it out of every other suite's run.
 *
 * @since 1.0.0
 */
class GreeterContractTest {
    /**
     * Verifies that the greeting carries the name it was given.
     *
     * @since 1.0.0
     */
    @Test
    @Tag("contract")
    fun greetsWithAName() {
        assert(Greeter().greet("world").contains("world"))
    }
}
