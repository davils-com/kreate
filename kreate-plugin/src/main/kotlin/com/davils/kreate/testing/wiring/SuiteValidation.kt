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

package com.davils.kreate.testing.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.gradle.SourceSetName
import com.davils.kreate.testing.LegacyTestPolicy
import com.davils.kreate.testing.TestSuiteExtension
import com.davils.kreate.testing.TestSuiteNames
import com.davils.kreate.testing.TestsExtension
import com.davils.kreate.testing.suite.enabledSuites
import org.gradle.api.GradleException
import org.gradle.api.Project

internal fun Project.validateSuites(extension: KreateExtension) {
    val tests = extension.project.tests
    val suites = tests.enabledSuites()
    val reserved = reservedSourceSetNames(extension, tests)
    val known = suites.map { it.name }.toSet()
    val taken = mutableMapOf<String, String>()

    val problem = suites.firstNotNullOfOrNull { suite -> suiteProblem(suite, reserved, taken, known) } ?: return
    throw GradleException(problem)
}

private fun reservedSourceSetNames(
    extension: KreateExtension,
    tests: TestsExtension
): Map<String, String> = buildMap {
    put(SourceSetName.MAIN, "the production source set")
    put(extension.project.benchmark.sourceSetName.get(), "the benchmark source set")

    val keepsLegacyTestSourceSet = tests.legacyTestSourceSet.get() == LegacyTestPolicy.KEEP
    if (keepsLegacyTestSourceSet) {
        put(TestSuiteNames.LEGACY, "the legacy test source set, which the current policy keeps")
    }
}

private fun Project.suiteProblem(
    suite: TestSuiteExtension,
    reserved: Map<String, String>,
    taken: MutableMap<String, String>,
    known: Set<String>
): String? {
    val sourceSetName = suite.sourceSetName.get()
    val reservedBy = reserved[sourceSetName]
    val duplicateOf = taken.put(sourceSetName, suite.name)
    val referencedSuites = suite.dependsOnSuites.get() + suite.mustRunAfterSuites.get()
    val unknownReference = referencedSuites.firstOrNull { it !in known }

    if (reservedBy != null) {
        return "Test suite '${suite.name}' in project '$path' uses the source set name " +
            "'$sourceSetName', which already belongs to $reservedBy. Give the suite a different " +
            "name, or set its `sourceSetName`."
    }

    if (duplicateOf != null) {
        return "Test suites '$duplicateOf' and '${suite.name}' in project '$path' both use the " +
            "source set name '$sourceSetName'. Source set names must be unique."
    }

    if (unknownReference != null) {
        return "Test suite '${suite.name}' in project '$path' refers to suite '$unknownReference', " +
            "which is not registered or not enabled. Known suites: ${known.sorted().joinToString()}"
    }

    return null
}
