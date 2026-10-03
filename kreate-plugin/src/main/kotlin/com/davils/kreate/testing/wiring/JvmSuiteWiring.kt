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
import com.davils.kreate.testing.TestSuiteExtension
import com.davils.kreate.testing.TestSuiteNames
import com.davils.kreate.testing.suite.SuitePlatform
import com.davils.kreate.testing.suite.enabledSuites
import com.davils.kreate.testing.suite.implementationName
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

internal fun Project.initializeJvmSuites(extension: KreateExtension) {
    val tests = extension.project.tests
    validateSuites(extension)

    val suites = tests.enabledSuites()
    suites.forEach { suite -> prepareJvmSuiteSourceSet(suite) }
    val tasksBySuite = suites.associate { suite -> suite.name to registerJvmSuiteTask(suite) }

    wireSuites(tests, tasksBySuite)

    val kotlin = extensions.getByType(KotlinJvmProjectExtension::class.java)
    applyLegacyTestPolicy(
        tests = tests,
        unitSuiteTask = tasksBySuite[TestSuiteNames.UNIT],
        legacySourceSets = legacyJvmSourceSets(kotlin),
        legacyTaskNames = setOf(TestSuiteNames.LEGACY)
    )
}

private fun Project.prepareJvmSuiteSourceSet(suite: TestSuiteExtension) {
    createJvmSuiteSourceSet(suite)

    val sourceSetName = suite.sourceSetName.get()
    addSuiteDependencies(suite, sourceSetName)
    addJUnitPlatformLauncher(suite, sourceSetName)
    addKotestBundle(suite, implementationName(sourceSetName), SuitePlatform.JVM)
}

private fun Project.registerJvmSuiteTask(suite: TestSuiteExtension): TaskProvider<Test> {
    val task = wireJvmSuite(suite)
    task.configure { configureSuite(suite) }
    return task
}

private fun legacyJvmSourceSets(kotlin: KotlinJvmProjectExtension): Map<String, KotlinSourceSet> {
    val legacySourceSet = kotlin.sourceSets.findByName(TestSuiteNames.LEGACY) ?: return emptyMap()
    return mapOf(TestSuiteNames.LEGACY to legacySourceSet)
}
