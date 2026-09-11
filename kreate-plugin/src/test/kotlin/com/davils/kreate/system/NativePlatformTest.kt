/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.kreate.system

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for the native platform helpers.
 *
 * The platform identifier decides which directory a native artifact is filed under, both
 * in the build directory and inside a published JAR. If it were ambiguous, a shared build
 * cache or a multi-architecture CI matrix would mix incompatible binaries.
 */
@DisplayName("NativePlatform")
class NativePlatformTest {

    @Test
    @DisplayName("produces an <os>-<arch> identifier for the running platform")
    fun platformIdShape() {
        currentPlatformId() shouldMatch Regex("(windows|linux|macos)-(x86_64|aarch64)")
    }

    @Test
    @DisplayName("is stable across calls")
    fun platformIdIsStable() {
        currentPlatformId() shouldBe currentPlatformId()
    }

    @Test
    @DisplayName("uses the platform's shared library naming convention")
    fun sharedLibraryNaming() {
        val os by getOs()
        val expected = when (os) {
            OsTarget.WINDOWS -> "example.dll"
            OsTarget.MACOS -> "libexample.dylib"
            else -> "libexample.so"
        }

        sharedLibraryFileName("example") shouldBe expected
    }

    @Test
    @DisplayName("detects a supported operating system")
    fun detectsOs() {
        val os by getOs()

        listOf(OsTarget.WINDOWS, OsTarget.LINUX, OsTarget.MACOS) shouldContain os
    }

    @Test
    @DisplayName("detects a supported architecture")
    fun detectsArchitecture() {
        val architecture by getArchitecture()

        listOf(Architecture.X64, Architecture.ARM64) shouldContain architecture
    }
}
