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

package com.davils.kreate.benchmark.task

import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import java.io.File

private const val JSON_REPORT_PATTERN = "**/*.json"

internal fun ConfigurableFileCollection.firstNormalizedReport(profile: String): File {
    val reports = asFileTree.matching { include(JSON_REPORT_PATTERN) }.files
    return reports.minByOrNull { it.name } ?: throw GradleException(missingReportMessage(profile))
}

private fun missingReportMessage(profile: String): String =
    listOf(
        "No normalized benchmark report is available for profile '$profile'.",
        "",
        "Run the benchmarks first:",
        "",
        "    ./gradlew benchmark"
    ).joinToString("\n")
