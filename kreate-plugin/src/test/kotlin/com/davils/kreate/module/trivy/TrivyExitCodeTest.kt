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

package com.davils.kreate.module.trivy

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.GradleException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Tests for how a Trivy exit code is read.
 *
 * Trivy exits with 1 when it fails and the Go runtime exits with 2 when it crashes. A scan that
 * took either for its findings code reported a crash as a finding, and one that ignored them
 * passed a scan that never ran - both happened in Rise, where concurrent scans corrupted the
 * vulnerability database.
 */
@DisplayName("Trivy exit code")
class TrivyExitCodeTest {

    private val lockFile = File("module/gradle.lockfile")

    @Test
    @DisplayName("0 is a clean scan")
    fun cleanScan() {
        trivyReportedFindings(0, lockFile) shouldBe false
    }

    @Test
    @DisplayName("the findings code is a scan with findings")
    fun findings() {
        trivyReportedFindings(TRIVY_FINDINGS_EXIT_CODE, lockFile) shouldBe true
    }

    @Test
    @DisplayName("1, Trivy's own failure, fails the task instead of counting as a finding")
    fun trivyFailure() {
        val failure = shouldThrow<GradleException> { trivyReportedFindings(1, lockFile) }

        failure.message shouldContain "module/gradle.lockfile"
        failure.message shouldContain "exited with 1"
    }

    @Test
    @DisplayName("2, a crash of the Go runtime, fails the task instead of passing it")
    fun runtimeCrash() {
        shouldThrow<GradleException> { trivyReportedFindings(2, lockFile) }
    }
}
