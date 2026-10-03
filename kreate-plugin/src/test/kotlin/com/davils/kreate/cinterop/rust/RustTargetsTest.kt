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

package com.davils.kreate.cinterop.rust

import com.davils.kreate.host.HostPlatform
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import org.gradle.api.GradleException

private const val LINUX: String = "Linux"

class RustTargetsTest : FunSpec({
    context("hostRustTarget") {
        withData(
            nameFn = { (osName, archName, target) -> "$osName on $archName builds $target" },
            Triple("Windows 11", "amd64", "x86_64-pc-windows-gnu"),
            Triple("Mac OS X", "aarch64", "aarch64-apple-darwin"),
            Triple(LINUX, "amd64", "x86_64-unknown-linux-gnu"),
            Triple(LINUX, "aarch64", "aarch64-unknown-linux-gnu")
        ) { (osName, archName, target) ->
            hostRustTarget(HostPlatform(osName, archName)) shouldBe target
        }

        test("rejects an operating system Kreate does not recognise") {
            val failure = shouldThrow<GradleException> { hostRustTarget(HostPlatform("Plan 9", "amd64")) }

            failure.message shouldBe "Cannot determine the Rust target: unsupported operating system 'Plan 9'."
        }

        test("rejects a Linux architecture Kreate does not recognise with a Gradle exception") {
            val failure = shouldThrow<GradleException> { hostRustTarget(HostPlatform(LINUX, "riscv64")) }

            failure.message shouldBe "Cannot determine the Rust target: unsupported architecture 'riscv64'."
        }
    }
})
