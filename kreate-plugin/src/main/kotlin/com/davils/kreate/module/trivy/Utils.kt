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

import com.davils.kreate.tooling.ExecutableResolver
import com.davils.kreate.tooling.ExternalTool
import org.gradle.api.GradleException
import java.io.File

internal const val TRIVY_FINDINGS_EXIT_CODE: Int = 10

private const val TRIVY_CLEAN_EXIT_CODE: Int = 0

/**
 * Resolves the path to the Trivy executable.
 *
 * Delegates to the shared [ExecutableResolver], which searches the `PATH` first and falls
 * back to the conventional installation directories on every platform.
 *
 * @return The resolved Trivy command path or name as a string.
 * @since 1.2.0
 */
internal fun resolveTrivyCommand(): String = ExecutableResolver.resolve(ExternalTool.TRIVY)

internal fun trivyReportedFindings(exitValue: Int, target: File): Boolean = when (exitValue) {
    TRIVY_CLEAN_EXIT_CODE -> false
    TRIVY_FINDINGS_EXIT_CODE -> true
    else -> throw GradleException(
        "Trivy did not finish scanning ${target.invariantSeparatorsPath}: it exited with $exitValue. " +
            "This is a failure of Trivy itself, not a finding - its output is above."
    )
}
