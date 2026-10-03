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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.GradleException

class KnownPlatformsTest : FunSpec({
    context("Known platforms") {
        test("covers every operating system and architecture combination") {
            val wireFormatLiterals = setOf(
                "windows-x86_64", "windows-aarch64",
                "linux-x86_64", "linux-aarch64",
                "macos-x86_64", "macos-aarch64"
            )

            KNOWN_PLATFORM_IDS shouldBe wireFormatLiterals
        }

        test("contains whatever the current machine reports") {
            (HostPlatform.current().platformId() in KNOWN_PLATFORM_IDS) shouldBe true
        }

        test("accepts a known identifier unchanged") {
            requireKnownPlatform("linux-x86_64", "test") shouldBe "linux-x86_64"
        }

        test("rejects a plausible typo and lists what is supported") {
            val osArchOnLinuxJvm = "linux-amd64"
            val failure = shouldThrow<GradleException> {
                requireKnownPlatform(osArchOnLinuxJvm, "jni { packaging { publishing { platforms } } }")
            }

            failure.message shouldContain "linux-amd64"
            failure.message shouldContain "linux-x86_64"
            failure.message shouldContain "jni { packaging { publishing { platforms } } }"
        }

        test("derives task names in upper camel case") {
            platformTaskSuffix("linux-x86_64") shouldBe "LinuxX86_64"
            platformTaskSuffix("macos-aarch64") shouldBe "MacosAarch64"
        }
    }
})
