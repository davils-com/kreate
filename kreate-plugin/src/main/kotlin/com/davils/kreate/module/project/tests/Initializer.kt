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

package com.davils.kreate.module.project.tests

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreateTasks
import com.davils.kreate.module.project.tests.logging.configureLogging
import com.davils.kreate.module.project.tests.report.configureReport
import com.davils.kreate.module.project.tests.suite.SuitePlatform
import com.davils.kreate.module.project.tests.suite.addJUnitPlatformLauncher
import com.davils.kreate.module.project.tests.suite.addKotestBundle
import com.davils.kreate.module.project.tests.suite.addSuiteDependencies
import com.davils.kreate.module.project.tests.suite.applyLegacyTestPolicy
import com.davils.kreate.module.project.tests.suite.applySuiteHierarchyTemplate
import com.davils.kreate.module.project.tests.suite.configureSuite
import com.davils.kreate.module.project.tests.suite.createJvmSuiteSourceSet
import com.davils.kreate.module.project.tests.suite.createKmpSuiteSourceSets
import com.davils.kreate.module.project.tests.suite.enabledSuites
import com.davils.kreate.module.project.tests.suite.sharedSourceSetName
import com.davils.kreate.module.project.tests.suite.validateSuites
import com.davils.kreate.module.project.tests.suite.wireJvmSuite
import com.davils.kreate.module.project.tests.suite.wireMultiplatformSuite
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinTest
import java.time.Duration

/**
 * The id of the Kotlin/JVM plugin.
 *
 * @since 3.0.0
 */
private const val KOTLIN_JVM_PLUGIN_ID: String = "org.jetbrains.kotlin.jvm"

/**
 * The id of the Kotlin Multiplatform plugin.
 *
 * @since 3.0.0
 */
private const val KOTLIN_MULTIPLATFORM_PLUGIN_ID: String = "org.jetbrains.kotlin.multiplatform"

/**
 * Initializes testing configuration for the project.
 *
 * Two things happen here, in this order. Every test task in the project, whoever created it,
 * gets the project-wide execution settings. Then each enabled suite is created - its source
 * set, its compilation wiring, its task - and its own settings are applied on top.
 *
 * The order is load bearing. Both passes call `useJUnitPlatform`, the later call wins, and the
 * tag filters only exist in the second one.
 *
 * @param extension The Kreate configuration extension.
 * @since 1.0.0
 */
internal fun Project.initializeTesting(extension: KreateExtension) {
    val testingExtension = extension.project.tests
    if (!testingExtension.enabled.get()) return

    configureAllTestTasks(testingExtension)

    plugins.withId(KOTLIN_JVM_PLUGIN_ID) { initializeJvmSuites(extension) }
    plugins.withId(KOTLIN_MULTIPLATFORM_PLUGIN_ID) { initializeMultiplatformSuites(extension) }
}

/**
 * Applies the project-wide test settings to every test task.
 *
 * @param tests The testing configuration.
 * @since 1.0.0
 */
private fun Project.configureAllTestTasks(tests: TestsExtension) {
    // Read out of the extension before the lambdas capture anything: an `upToDateWhen` spec is
    // serialised into the configuration cache entry, and a lambda holding the extension takes
    // the project model with it.
    val runTimeout = Duration.ofMinutes(tests.timeoutMinutes.get())
    val ignoreTestFailures = tests.ignoreFailures.get()
    val failOnEmpty = tests.failOnNoDiscoveredTests.get()
    val alwaysRun = tests.alwaysRunTests.get()
    val forks = tests.maxParallelForks.get()

    fun AbstractTestTask.configureCommon() {
        timeout.set(runTimeout)
        ignoreFailures = ignoreTestFailures
        failOnNoDiscoveredTests.set(failOnEmpty)
        outputs.upToDateWhen { !alwaysRun }
        configureLogging(tests.logging)
        configureReport(tests.report)
    }

    val kotlinPlugins = listOf(KOTLIN_JVM_PLUGIN_ID, KOTLIN_MULTIPLATFORM_PLUGIN_ID)
    kotlinPlugins.forEach { pluginId ->
        plugins.withId(pluginId) {
            tasks.withType<Test>().configureEach {
                configureCommon()
                useJUnitPlatform()
                maxParallelForks = forks
            }
        }
    }

    // Kotlin's own test tasks for the non-JVM targets are not `Test` tasks and know nothing
    // about the JUnit Platform, so they take the shared settings and nothing else.
    plugins.withId(KOTLIN_MULTIPLATFORM_PLUGIN_ID) {
        tasks.withType<KotlinTest>().configureEach { configureCommon() }
    }
}

/**
 * Creates the suites of a Kotlin/JVM project.
 *
 * @param extension The Kreate configuration extension.
 * @since 3.0.0
 */
private fun Project.initializeJvmSuites(extension: KreateExtension) {
    val tests = extension.project.tests
    validateSuites(extension)

    // Two passes. A container hands its elements back in alphabetical order, so
    // `integrationTest` is reached before the `unitTest` it may associate with; every source
    // set has to exist before anything is wired to anything else.
    val suites = tests.enabledSuites()
    suites.forEach { suite ->
        createJvmSuiteSourceSet(suite)
        addSuiteDependencies(suite, suite.sourceSetName.get())
        addJUnitPlatformLauncher(suite, suite.sourceSetName.get())
        addKotestBundle(suite, "${suite.sourceSetName.get()}Implementation", SuitePlatform.JVM)
    }

    val tasksBySuite = suites.associate { suite ->
        val task = wireJvmSuite(suite)
        task.configure { configureSuite(suite) }
        suite.name to task
    }

    wireSuites(tests, tasksBySuite)

    val kotlin = extensions.getByType(KotlinJvmProjectExtension::class.java)
    applyLegacyTestPolicy(
        tests = tests,
        unitSuiteTask = tasksBySuite[KreateTasks.Tests.UNIT],
        legacySourceSets = legacyJvmSourceSets(kotlin),
        legacyTaskNames = setOf(KreateTasks.Tests.LEGACY)
    )
}

/**
 * Creates the suites of a Kotlin Multiplatform project.
 *
 * @param extension The Kreate configuration extension.
 * @since 3.0.0
 */
private fun Project.initializeMultiplatformSuites(extension: KreateExtension) {
    val tests = extension.project.tests
    validateSuites(extension)

    val kotlin = extensions.getByType(KotlinMultiplatformExtension::class.java)

    // Two passes, for the same reason as on Kotlin/JVM: a container iterates alphabetically,
    // so a suite is reached before the one it associates with.
    val suites = tests.enabledSuites()

    // Before any source set is created: the template decides which trees get wired, and the
    // Kotlin plugin only applies its own default template if nobody else applied one.
    kotlin.applySuiteHierarchyTemplate(suites)

    suites.forEach { suite ->
        val sharedName = sharedSourceSetName(suite.sourceSetName.get())
        createKmpSuiteSourceSets(suite, kotlin)
        addSuiteDependencies(suite, sharedName)
        addKotestBundle(suite, "${sharedName}Implementation", SuitePlatform.COMMON)
    }

    val tasksBySuite = suites.associate { suite ->
        val targetTasks = wireMultiplatformSuite(suite, kotlin)
        targetTasks.forEach { task ->
            task.configure { configureSuite(suite) }
            addJUnitPlatformLauncher(suite, task.name)
        }

        // An aggregate so that `./gradlew unitTest` means the same thing on a multiplatform
        // project as it does on a JVM one, whatever the per-target task is called.
        val aggregate = tasks.register(suite.name) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = suite.description.get()
            dependsOn(targetTasks)
        }
        suite.name to aggregate
    }

    wireSuites(tests, tasksBySuite)

    applyLegacyTestPolicy(
        tests = tests,
        unitSuiteTask = tasksBySuite[KreateTasks.Tests.UNIT],
        legacySourceSets = legacyMultiplatformSourceSets(kotlin),
        legacyTaskNames = legacyKmpTestTaskNames(kotlin)
    )
}

/**
 * Wires the suites into `check` and into each other's execution order.
 *
 * @param tests The testing configuration.
 * @param tasksBySuite The task of each enabled suite, by suite name.
 * @since 3.0.0
 */
private fun Project.wireSuites(tests: TestsExtension, tasksBySuite: Map<String, TaskProvider<out Task>>) {
    tests.enabledSuites().forEach { suite ->
        val task = tasksBySuite[suite.name] ?: return@forEach

        if (suite.runOnCheck.get()) {
            tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure { dependsOn(task) }
        }

        // Ordering only. `dependsOn` would mean that asking for the integration suite drags the
        // unit suite into the build, which defeats the point of being able to run one of them.
        suite.mustRunAfterSuites.get().forEach { other ->
            tasksBySuite[other]?.let { earlier -> task.configure { mustRunAfter(earlier) } }
        }
    }
}

/**
 * Returns the conventional test source sets of a Kotlin/JVM project.
 *
 * @param kotlin The Kotlin/JVM extension.
 * @return The legacy source sets, by name.
 * @since 3.0.0
 */
private fun legacyJvmSourceSets(kotlin: KotlinJvmProjectExtension): Map<String, KotlinSourceSet> =
    kotlin.sourceSets.findByName(KotlinCompilation.TEST_COMPILATION_NAME)
        ?.let { mapOf(KotlinCompilation.TEST_COMPILATION_NAME to it) }
        ?: emptyMap()

/**
 * Returns the conventional test source sets of a multiplatform project.
 *
 * @param kotlin The multiplatform extension.
 * @return The legacy source sets, by name.
 * @since 3.0.0
 */
private fun legacyMultiplatformSourceSets(
    kotlin: KotlinMultiplatformExtension
): Map<String, KotlinSourceSet> {
    val names = listOf("commonTest") + kotlin.targets.map { "${it.name}Test" }
    return names.mapNotNull { name -> kotlin.sourceSets.findByName(name)?.let { name to it } }.toMap()
}

/**
 * Returns the names of the conventional test tasks of a multiplatform project.
 *
 * @param kotlin The multiplatform extension.
 * @return The legacy test task names.
 * @since 3.0.0
 */
private fun legacyKmpTestTaskNames(kotlin: KotlinMultiplatformExtension): Set<String> =
    kotlin.targets.map { "${it.name}Test" }.toSet()
