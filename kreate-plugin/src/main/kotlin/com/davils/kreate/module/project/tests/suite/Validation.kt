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

package com.davils.kreate.module.project.tests.suite

import com.davils.kreate.KreateExtension
import com.davils.kreate.module.project.tests.LegacyTestPolicy
import com.davils.kreate.module.project.tests.TestsExtension
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Returns the suites that are configured and enabled.
 *
 * @return The enabled suites, in registration order.
 * @since 3.0.0
 */
internal fun TestsExtension.enabledSuites(): List<TestSuiteExtension> =
    suites.filter { it.enabled.get() }

/**
 * Returns the source set names a suite may not take, mapped to what already owns them.
 *
 * `maybeCreate` is why this check exists rather than a failure at creation time: asked for a
 * name that is already taken it hands back the existing source set, so a suite called `main`
 * would quietly turn the production code into a test suite and run it.
 *
 * @param extension The Kreate configuration extension.
 * @param tests The testing configuration.
 * @return The reserved source set names and their owners.
 * @since 3.0.0
 */
private fun reservedSourceSetNames(
    extension: KreateExtension,
    tests: TestsExtension
): Map<String, String> = buildMap {
    put("main", "the production source set")
    put(extension.project.benchmark.sourceSetName.get(), "the benchmark source set")
    if (tests.legacyTestSourceSet.get() == LegacyTestPolicy.KEEP) {
        put("test", "the legacy test source set, which the current policy keeps")
    }
}

/**
 * Fails the build if any enabled suite is misconfigured.
 *
 * Runs before a single source set is created, because every failure it reports is one that
 * would otherwise be silent: a name collision hands back somebody else's source set, and a
 * reference to a suite that does not exist produces a suite without the fixtures it asked
 * for rather than an error.
 *
 * @param extension The Kreate configuration extension.
 * @throws GradleException When a suite name is reserved or duplicated, or refers to a suite
 * that is not registered.
 * @since 3.0.0
 */
internal fun Project.validateSuites(extension: KreateExtension) {
    val tests = extension.project.tests
    val suites = tests.enabledSuites()
    val reserved = reservedSourceSetNames(extension, tests)
    val known = suites.map { it.name }.toSet()
    val taken = mutableMapOf<String, String>()

    val problem = suites.firstNotNullOfOrNull { suite ->
        val sourceSetName = suite.sourceSetName.get()

        val reservedBy = reserved[sourceSetName]
        val duplicateOf = taken.put(sourceSetName, suite.name)
        val unknownReference = (suite.dependsOnSuites.get() + suite.mustRunAfterSuites.get())
            .firstOrNull { it !in known }

        when {
            reservedBy != null -> "Test suite '${suite.name}' in project '$path' uses the source " +
                "set name '$sourceSetName', which already belongs to $reservedBy. Give the suite " +
                "a different name, or set its `sourceSetName`."

            duplicateOf != null -> "Test suites '$duplicateOf' and '${suite.name}' in project " +
                "'$path' both use the source set name '$sourceSetName'. Source set names must " +
                "be unique."

            unknownReference != null -> "Test suite '${suite.name}' in project '$path' refers to " +
                "suite '$unknownReference', which is not registered or not enabled. Known " +
                "suites: ${known.sorted().joinToString()}"

            else -> null
        }
    }

    if (problem != null) throw GradleException(problem)
}
