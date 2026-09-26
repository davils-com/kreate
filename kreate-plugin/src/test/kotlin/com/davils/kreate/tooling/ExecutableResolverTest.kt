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

package com.davils.kreate.tooling

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Tests for [ExecutableResolver].
 */
@DisplayName("ExecutableResolver")
class ExecutableResolverTest {

    private val pathDirectories: List<String> =
        System.getenv("PATH").orEmpty().split(File.pathSeparatorChar).filter { it.isNotBlank() }

    /**
     * Whether [command] is installed in one of [directories], independently of the resolver.
     *
     * On Windows the file is `trivy.exe` rather than `trivy`. Looking for the bare name alone took
     * an installed tool for a missing one there, so the fallback test ran against a real Trivy.
     */
    private fun installed(command: String, directories: List<String>): Boolean {
        val names = listOf(command, "$command.exe", "$command.cmd", "$command.bat")
        return directories.any { directory -> names.any { File(directory, it).canExecute() } }
    }

    @Test
    @DisplayName("an explicit override always wins")
    fun overrideWins() {
        ExecutableResolver.resolve(ExternalTool.CMAKE, "/opt/custom/cmake") shouldBe "/opt/custom/cmake"
    }

    @Test
    @DisplayName("a non-existent override is returned unchanged so the failure names the user's own setting")
    fun overrideIsNotValidated() {
        // Silently falling back here would hide a typo in the build script behind a
        // completely different toolchain being used.
        ExecutableResolver.resolve(ExternalTool.CMAKE, "/does/not/exist/cmake") shouldBe
            "/does/not/exist/cmake"
    }

    @Test
    @DisplayName("a blank override is ignored")
    fun blankOverrideIgnored() {
        ExecutableResolver.resolve(ExternalTool.CMAKE, "   ") shouldNotBe "   "
    }

    @Test
    @DisplayName("resolves an absolute path for a tool that is installed")
    fun resolvesInstalledTool() {
        assumeTrue(installed("cmake", pathDirectories), "CMake is not installed on this machine")

        val resolved = ExecutableResolver.resolve(ExternalTool.CMAKE)

        File(resolved).isAbsolute shouldBe true
        File(resolved).canExecute() shouldBe true
    }

    @Test
    @DisplayName("falls back to the bare command name for a tool that is not installed")
    fun fallsBackToCommandName() {
        val searched = pathDirectories + ExternalTool.TRIVY.wellKnownDirectories
        assumeTrue(!installed("trivy", searched), "Trivy is installed, so the fallback cannot be observed")

        // Returning the bare name lets the operating system produce the error message,
        // which names the tool rather than an invented path.
        ExecutableResolver.resolve(ExternalTool.TRIVY) shouldBe "trivy"
    }

    @Test
    @DisplayName("every tool declares at least one well known directory")
    fun toolsDeclareSearchPaths() {
        ExternalTool.entries.forEach { tool ->
            (tool.wellKnownDirectories.isNotEmpty() || tool.homeRelativeDirectories.isNotEmpty()) shouldBe true
        }
    }

    @Test
    @DisplayName("cargo is searched in the rustup location on every platform")
    fun cargoSearchesRustupDirectory() {
        // The previous implementation hard-coded ~/.cargo/bin on macOS only, so a Linux
        // agent without cargo on PATH silently failed.
        ExternalTool.CARGO.homeRelativeDirectories shouldBe listOf(".cargo/bin")
    }
}
