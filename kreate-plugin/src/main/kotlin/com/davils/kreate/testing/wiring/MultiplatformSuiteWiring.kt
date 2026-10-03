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
import com.davils.kreate.testing.suite.sharedSourceSetName
import com.davils.kreate.testing.task.SuiteJvmTest
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

private const val LEGACY_TEST_SUFFIX: String = "Test"

internal fun Project.initializeMultiplatformSuites(extension: KreateExtension) {
    val tests = extension.project.tests
    validateSuites(extension)

    val kotlin = extensions.getByType(KotlinMultiplatformExtension::class.java)
    val suites = tests.enabledSuites()
    kotlin.applySuiteHierarchyTemplate(suites)

    suites.forEach { suite -> prepareSuiteSourceSets(suite, kotlin) }
    val tasksBySuite = suites.associate { suite -> suite.name to registerSuiteTasks(suite, kotlin) }

    wireSuites(tests, tasksBySuite)

    applyLegacyTestPolicy(
        tests = tests,
        unitSuiteTask = tasksBySuite[TestSuiteNames.UNIT],
        legacySourceSets = legacyMultiplatformSourceSets(kotlin),
        legacyTaskNames = legacyTestTaskNames(kotlin)
    )
}

private fun Project.prepareSuiteSourceSets(
    suite: TestSuiteExtension,
    kotlin: KotlinMultiplatformExtension
) {
    val sharedName = sharedSourceSetName(suite.sourceSetName.get())
    createKmpSuiteSourceSets(suite, kotlin)
    addSuiteDependencies(suite, sharedName)
    addKotestBundle(suite, implementationName(sharedName), SuitePlatform.COMMON)
}

private fun Project.registerSuiteTasks(
    suite: TestSuiteExtension,
    kotlin: KotlinMultiplatformExtension
): TaskProvider<Task> {
    val targetTasks = wireMultiplatformSuite(suite, kotlin)
    targetTasks.forEach { task ->
        task.configure { configureSuite(suite) }
        addJUnitPlatformLauncher(suite, task.name)
    }

    return registerSuiteLifecycleTask(suite, targetTasks)
}

private fun Project.registerSuiteLifecycleTask(
    suite: TestSuiteExtension,
    targetTasks: List<TaskProvider<SuiteJvmTest>>
): TaskProvider<Task> = tasks.register(suite.name) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = suite.description.get()
    dependsOn(targetTasks)
}

private fun legacyMultiplatformSourceSets(kotlin: KotlinMultiplatformExtension): Map<String, KotlinSourceSet> {
    val names = listOf(sharedSourceSetName(TestSuiteNames.LEGACY)) + legacyTargetTestNames(kotlin)
    return names.mapNotNull { name -> kotlin.sourceSets.findByName(name)?.let { name to it } }.toMap()
}

private fun legacyTestTaskNames(kotlin: KotlinMultiplatformExtension): Set<String> =
    legacyTargetTestNames(kotlin).toSet()

private fun legacyTargetTestNames(kotlin: KotlinMultiplatformExtension): List<String> =
    kotlin.targets.map { target -> "${target.name}$LEGACY_TEST_SUFFIX" }
