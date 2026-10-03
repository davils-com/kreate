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

package com.davils.kreate.trivy.task

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.GradleException
import java.io.File

private const val TRIVY_FAILURE_EXIT_CODE = 1
private const val GO_RUNTIME_CRASH_EXIT_CODE = 2

class TrivyExitCodeTest : FunSpec({
    val lockFile = File("module/gradle.lockfile")

    context("Trivy exit code") {
        test("0 is a clean scan") {
            trivyReportedFindings(TRIVY_CLEAN_EXIT_CODE, lockFile) shouldBe false
        }

        test("the findings code is a scan with findings") {
            trivyReportedFindings(TRIVY_FINDINGS_EXIT_CODE, lockFile) shouldBe true
        }

        test("1, Trivy's own failure, fails the task instead of counting as a finding") {
            val failure = shouldThrow<GradleException> {
                trivyReportedFindings(TRIVY_FAILURE_EXIT_CODE, lockFile)
            }

            failure.message shouldContain "module/gradle.lockfile"
            failure.message shouldContain "exited with 1"
        }

        test("2, a crash of the Go runtime, fails the task instead of passing it") {
            shouldThrow<GradleException> { trivyReportedFindings(GO_RUNTIME_CRASH_EXIT_CODE, lockFile) }
        }
    }
})
