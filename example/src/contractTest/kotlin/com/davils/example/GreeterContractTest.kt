package com.davils.example

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class GreeterContractTest {
    @Test
    @Tag("contract")
    fun greetsWithAName() {
        assert(Greeter().greet("world").contains("world"))
    }
}
