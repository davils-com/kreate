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
import com.davils.kreate.gradle.KotlinPluginId
import com.davils.kreate.testing.TestSuiteExtension
import com.davils.kreate.testing.TestsExtension
import com.davils.kreate.testing.suite.enabledSuites
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinTest

internal fun Project.initializeTesting(extension: KreateExtension) {
    val testingExtension = extension.project.tests
    if (!testingExtension.enabled.get()) return

    configureAllTestTasks(testingExtension)

    plugins.withId(KotlinPluginId.JVM) { initializeJvmSuites(extension) }
    plugins.withId(KotlinPluginId.MULTIPLATFORM) { initializeMultiplatformSuites(extension) }
}

internal fun Project.wireSuites(tests: TestsExtension, tasksBySuite: Map<String, TaskProvider<out Task>>) {
    tests.enabledSuites().forEach { suite ->
        tasksBySuite[suite.name]?.let { task -> wireSuite(suite, task, tasksBySuite) }
    }
}

private fun Project.wireSuite(
    suite: TestSuiteExtension,
    task: TaskProvider<out Task>,
    tasksBySuite: Map<String, TaskProvider<out Task>>
) {
    if (suite.runOnCheck.get()) {
        tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure { dependsOn(task) }
    }

    val earlierSuiteTasks = suite.mustRunAfterSuites.get().mapNotNull { other -> tasksBySuite[other] }
    earlierSuiteTasks.forEach { earlier -> task.configure { mustRunAfter(earlier) } }
}

private fun Project.configureAllTestTasks(tests: TestsExtension) {
    val settings = tests.sharedTestTaskSettings()

    val kotlinPluginIds = listOf(KotlinPluginId.JVM, KotlinPluginId.MULTIPLATFORM)
    kotlinPluginIds.forEach { pluginId ->
        plugins.withId(pluginId) { configureJUnitPlatformTasks(tests, settings) }
    }

    plugins.withId(KotlinPluginId.MULTIPLATFORM) { configureNonJvmKotlinTestTasks(tests, settings) }
}

private fun Project.configureJUnitPlatformTasks(tests: TestsExtension, settings: SharedTestTaskSettings) {
    tasks.withType<Test>().configureEach {
        applySharedSettings(tests, settings)
        useJUnitPlatform()
        maxParallelForks = settings.maxParallelForks
    }
}

private fun Project.configureNonJvmKotlinTestTasks(tests: TestsExtension, settings: SharedTestTaskSettings) {
    tasks.withType<KotlinTest>().configureEach { applySharedSettings(tests, settings) }
}

private fun AbstractTestTask.applySharedSettings(tests: TestsExtension, settings: SharedTestTaskSettings) {
    val alwaysRun = settings.alwaysRun

    timeout.set(settings.timeout)
    ignoreFailures = settings.ignoreFailures
    failOnNoDiscoveredTests.set(settings.failOnNoDiscoveredTests)
    outputs.upToDateWhen { !alwaysRun }
    configureLogging(tests.logging)
    configureReport(tests.report)
}
