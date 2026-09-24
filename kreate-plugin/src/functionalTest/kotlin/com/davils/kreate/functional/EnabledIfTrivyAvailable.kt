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

package com.davils.kreate.functional

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File

/**
 * Skips a test when Trivy is not installed.
 *
 * Mirrors [EnabledIfCmakeAvailable]: a developer can run the suite without Trivy, and setting
 * `KREATE_REQUIRE_TRIVY` turns a missing binary into a failure instead of a skip, so CI cannot pass by
 * quietly not running the scan.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(TrivyAvailableCondition::class)
annotation class EnabledIfTrivyAvailable

/**
 * The [ExecutionCondition] backing [EnabledIfTrivyAvailable].
 */
class TrivyAvailableCondition : ExecutionCondition {

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        if (trivyAvailable) {
            return ConditionEvaluationResult.enabled("Trivy is available")
        }

        check(System.getenv("KREATE_REQUIRE_TRIVY") == null) {
            "Trivy is required but was not found on PATH. The secret scan tests must not be " +
                "skipped when KREATE_REQUIRE_TRIVY is set."
        }

        return ConditionEvaluationResult.disabled("Trivy is not installed")
    }

    private companion object {
        val trivyAvailable: Boolean by lazy {
            val names = if (System.getProperty("os.name").lowercase().contains("win")) {
                listOf("trivy.exe", "trivy")
            } else {
                listOf("trivy")
            }
            val wellKnown = listOf("/opt/homebrew/bin", "/usr/local/bin", "/usr/bin")

            val onPath = System.getenv("PATH").orEmpty()
                .split(File.pathSeparatorChar)
                .filter { it.isNotBlank() }
            (onPath + wellKnown).any { directory -> names.any { File(directory, it).canExecute() } }
        }
    }
}
