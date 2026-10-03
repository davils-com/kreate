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

package com.davils.kreate.coverage.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.coverage.CoverageExtension
import com.davils.kreate.testing.suite.enabledSuites
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import kotlinx.kover.gradle.plugin.dsl.KoverReportSetConfig
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

internal fun Project.initializeCoverage(extension: KreateExtension) {
    val coverageExtension = extension.project.coverage
    if (!coverageExtension.enabled.get()) return

    extensions.configure<KoverProjectExtension> {
        configureEngine(coverageExtension)
        configureCurrentProject(coverageExtension, suiteSourceSetsToExclude(extension))
        reports { total { configureTotal(coverageExtension) } }
    }

    configureAggregation(coverageExtension.aggregate)
}

private fun KoverReportSetConfig.configureTotal(extension: CoverageExtension) {
    configureFilters(extension.filters)
    configureReports(extension.reports)
    verify { configureVerification(extension.verify) }
}

private fun suiteSourceSetsToExclude(extension: KreateExtension): Set<String> {
    val tests = extension.project.tests
    val includedSourceSets = extension.project.coverage.sources.includedSourceSets.get()
    val isExplicitlyIncluded = includedSourceSets.isNotEmpty()
    val isApplicable = tests.enabled.get() && tests.excludeSuitesFromCoverage.get() && !isExplicitlyIncluded
    if (!isApplicable) return emptySet()

    return tests.enabledSuites().mapTo(mutableSetOf()) { it.sourceSetName.get() }
}

private fun KoverProjectExtension.configureEngine(extension: CoverageExtension) {
    useJacoco.set(extension.useJacoco)
    if (extension.jacocoVersion.isPresent) jacocoVersion.set(extension.jacocoVersion)
}

private fun KoverProjectExtension.configureCurrentProject(
    extension: CoverageExtension,
    suiteSourceSets: Set<String>
) {
    currentProject {
        sources {
            excludeJava.set(extension.sources.excludeJava)
            includedSourceSets.set(extension.sources.includedSourceSets)
            excludedSourceSets.set(extension.sources.excludedSourceSets.map { it + suiteSourceSets })
        }

        instrumentation {
            disabledForAll.set(extension.instrumentation.disabledForAll)
            disabledForTestTasks.set(extension.instrumentation.disabledForTestTasks)
            includedClasses.set(extension.instrumentation.includedClasses)
            excludedClasses.set(extension.instrumentation.excludedClasses)
        }
    }
}
