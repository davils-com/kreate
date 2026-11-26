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

package com.davils.kreate.host

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import org.gradle.api.GradleException

private const val LINUX: String = "Linux"

private const val AMD64: String = "amd64"

class HostPlatformTest : FunSpec({
    context("HostPlatform.current") {
        test("produces an <os>-<arch> identifier for the running platform") {
            HostPlatform.current().platformId() shouldMatch Regex("(windows|linux|macos)-(x86_64|aarch64)")
        }

        test("detects a supported operating system") {
            val host = HostPlatform.current()

            host.operatingSystemOrNull().shouldNotBeNull()
        }
    }

    context("HostPlatform operating system") {
        withData(
            nameFn = { (osName, _) -> osName },
            "Windows 11" to OperatingSystem.WINDOWS,
            "Windows Server 2022" to OperatingSystem.WINDOWS,
            LINUX to OperatingSystem.LINUX,
            "Mac OS X" to OperatingSystem.MACOS,
            "Darwin" to OperatingSystem.MACOS
        ) { (osName, expected) ->
            HostPlatform(osName, AMD64).operatingSystemOrNull() shouldBe expected
        }

        test("reports no operating system for an unknown name") {
            HostPlatform("Plan 9", AMD64).operatingSystemOrNull() shouldBe null
        }

        test("reports no operating system for a Unix Kreate builds nothing for") {
            listOf("FreeBSD", "SunOS").forEach { osName ->
                HostPlatform(osName, AMD64).operatingSystemOrNull() shouldBe null
            }
        }

        test("reports no operating system when the name is missing") {
            HostPlatform(null, AMD64).operatingSystemOrNull() shouldBe null
        }

        test("requires a known operating system and names the purpose and the reported name") {
            val failure = shouldThrow<GradleException> {
                HostPlatform("Plan 9", AMD64).requireOperatingSystem("the test target")
            }

            failure.message shouldBe "Cannot determine the test target: unsupported operating system \"Plan 9\"."
        }

        test("escapes the reported name") {
            val failure = shouldThrow<GradleException> {
                HostPlatform("Plan\u202E9\n", AMD64).requireOperatingSystem("the test target")
            }

            failure.message shouldBe
                "Cannot determine the test target: unsupported operating system \"Plan\\u{202E}9\\n\"."
        }

        test("returns the known operating system when required") {
            HostPlatform(LINUX, AMD64).requireOperatingSystem("the test target") shouldBe OperatingSystem.LINUX
        }
    }

    context("HostPlatform architecture") {
        withData(
            nameFn = { (archName, _) -> archName },
            AMD64 to Architecture.X64,
            "x86_64" to Architecture.X64,
            "aarch64" to Architecture.ARM64,
            "arm64" to Architecture.ARM64,
            "AARCH64" to Architecture.ARM64
        ) { (archName, expected) ->
            HostPlatform(LINUX, archName).architectureOrNull() shouldBe expected
        }

        withData("riscv64", "ppc64le", "s390x") { archName ->
            HostPlatform(LINUX, archName).architectureOrNull() shouldBe null
        }

        test("reports no architecture when the name is missing") {
            HostPlatform(LINUX, null).architectureOrNull() shouldBe null
        }

        test("requires a known architecture and names the purpose and the reported name") {
            val failure = shouldThrow<GradleException> {
                HostPlatform(LINUX, "riscv64").requireArchitecture("the test target")
            }

            failure.message shouldBe "Cannot determine the test target: unsupported architecture \"riscv64\"."
        }

        test("returns the known architecture when required") {
            HostPlatform(LINUX, "aarch64").requireArchitecture("the test target") shouldBe Architecture.ARM64
        }
    }

    context("HostPlatform.platformId") {
        withData(
            nameFn = { (osName, archName, _) -> "$osName on $archName" },
            Triple("Windows 11", AMD64, "windows-x86_64"),
            Triple("Windows 11", "aarch64", "windows-aarch64"),
            Triple(LINUX, AMD64, "linux-x86_64"),
            Triple(LINUX, "aarch64", "linux-aarch64"),
            Triple("Mac OS X", "x86_64", "macos-x86_64"),
            Triple("Mac OS X", "aarch64", "macos-aarch64")
        ) { (osName, archName, expected) ->
            HostPlatform(osName, archName).platformId() shouldBe expected
        }

        test("fails with a Gradle exception on an unknown operating system") {
            val failure = shouldThrow<GradleException> { HostPlatform("Plan 9", AMD64).platformId() }

            failure.message shouldContain "the native platform"
            failure.message shouldContain "unsupported operating system \"Plan 9\""
        }

        test("fails with a Gradle exception on an unknown architecture") {
            val failure = shouldThrow<GradleException> { HostPlatform(LINUX, "riscv64").platformId() }

            failure.message shouldContain "the native platform"
            failure.message shouldContain "unsupported architecture \"riscv64\""
        }
    }

    context("OperatingSystem") {
        test("recognises the names the JVM reports, through Knot") {
            HostPlatform("windows 11", AMD64).operatingSystemOrNull() shouldBe OperatingSystem.WINDOWS
            HostPlatform("linux", AMD64).operatingSystemOrNull() shouldBe OperatingSystem.LINUX
            HostPlatform("mac os x", AMD64).operatingSystemOrNull() shouldBe OperatingSystem.MACOS
        }

        test("uses the wire format identifiers") {
            OperatingSystem.entries.map { system -> system.id } shouldBe listOf("windows", "linux", "macos")
        }
    }

    context("Architecture") {
        test("recognises both spellings of each architecture, through Knot") {
            listOf("amd64", "x86_64").forEach { name ->
                HostPlatform(LINUX, name).architectureOrNull() shouldBe Architecture.X64
            }
            listOf("aarch64", "arm64").forEach { name ->
                HostPlatform(LINUX, name).architectureOrNull() shouldBe Architecture.ARM64
            }
        }

        test("uses the wire format identifiers") {
            Architecture.X64.id shouldBe "x86_64"
            Architecture.ARM64.id shouldBe "aarch64"
        }
    }
})
