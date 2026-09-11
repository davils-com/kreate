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

import com.davils.kreate.KreateTasks
import com.davils.kreate.module.project.tests.LegacySourceDirectories
import com.davils.kreate.module.project.tests.LegacyTestPolicy
import com.davils.kreate.module.project.tests.TestsExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

/**
 * The file extensions that count as sources when deciding whether a legacy directory is empty.
 *
 * @since 3.0.0
 */
private val SOURCE_EXTENSIONS: Set<String> = setOf("kt", "kts", "java")

/**
 * Applies the configured policy to the conventional `test` source set and task.
 *
 * @param tests The testing configuration.
 * @param unitSuiteTask The unit suite's task, used by [LegacyTestPolicy.ALIAS].
 * @param legacySourceSets The Kotlin source sets the policy governs, by source set name.
 * @param legacyTaskNames The names of the test tasks the policy governs.
 * @throws GradleException When the policy is [LegacyTestPolicy.FAIL] and legacy sources remain.
 * @since 3.0.0
 */
internal fun Project.applyLegacyTestPolicy(
    tests: TestsExtension,
    unitSuiteTask: TaskProvider<out Task>?,
    legacySourceSets: Map<String, KotlinSourceSet>,
    legacyTaskNames: Set<String>
) {
    when (tests.legacyTestSourceSet.get()) {
        LegacyTestPolicy.FAIL -> {
            failOnLegacySources(legacySourceSets)
        }

        LegacyTestPolicy.DISABLE -> {
            disableLegacyTestTasks(legacyTaskNames)
            removeFromCheck(legacyTaskNames)
        }

        LegacyTestPolicy.ALIAS -> {
            // Disabled and carrying the dependency rather than left enabled: once the legacy
            // directories are cleared or adopted there is nothing for it to run, and Gradle
            // executes the dependencies of a disabled task regardless - so `./gradlew test`
            // and `check` both end up running the unit suite.
            disableLegacyTestTasks(legacyTaskNames)
            unitSuiteTask?.let { unit ->
                legacyTaskNames.forEach { name ->
                    tasks.matching { it.name == name }.configureEach { dependsOn(unit) }
                }
            }
        }

        LegacyTestPolicy.KEEP -> {
            // Nothing to do: the conventional source set and task stay exactly as they are.
        }
    }

    applyLegacySourceDirectories(tests, legacySourceSets)
}

/**
 * Moves or clears the legacy source directories according to [TestsExtension.legacySourceDirectories].
 *
 * @param tests The testing configuration.
 * @param legacySourceSets The Kotlin source sets the policy governs, by source set name.
 * @since 3.0.0
 */
private fun Project.applyLegacySourceDirectories(
    tests: TestsExtension,
    legacySourceSets: Map<String, KotlinSourceSet>
) {
    val mode = tests.legacySourceDirectories.get()
    if (mode == LegacySourceDirectories.KEEP) return

    val unitSuite = tests.enabledSuites().firstOrNull { it.name == KreateTasks.Tests.UNIT }

    legacySourceSets.forEach { (legacyName, legacySourceSet) ->
        if (mode == LegacySourceDirectories.ADOPT && unitSuite != null) {
            adoptInto(unitSuite, legacyName, legacySourceSet)
        }
        legacySourceSet.kotlin.setSrcDirs(emptyList<File>())
        legacySourceSet.resources.setSrcDirs(emptyList<File>())
    }
}

/**
 * Adds a legacy source set's directories to the matching source set of the unit suite.
 *
 * Matching means same tier: `commonTest` is adopted by `commonUnitTest` and `jvmTest` by
 * `jvmUnitTest`, so shared tests stay shared instead of being pinned to one target.
 *
 * @param unitSuite The unit suite.
 * @param legacyName The legacy source set name.
 * @param legacySourceSet The legacy source set.
 * @since 3.0.0
 */
private fun Project.adoptInto(
    unitSuite: TestSuiteExtension,
    legacyName: String,
    legacySourceSet: KotlinSourceSet
) {
    val suiteSourceSetName = unitSuite.sourceSetName.get()
    val targetName = if (legacyName == KreateTasks.Tests.LEGACY) {
        suiteSourceSetName
    } else {
        lowerCamelCaseName(legacyName.removeSuffix("Test"), suiteSourceSetName)
    }

    val destination = multiplatformSourceSets()?.findByName(targetName)
        ?: multiplatformSourceSets()?.findByName(suiteSourceSetName)
        ?: return

    val adopted = legacySourceSet.kotlin.srcDirs
    destination.kotlin.srcDirs(adopted)
    destination.resources.srcDirs(legacySourceSet.resources.srcDirs)
}

/**
 * Returns the Kotlin source sets of this project, whichever Kotlin plugin is applied.
 *
 * @return The source set container, or `null` when no Kotlin plugin is applied.
 * @since 3.0.0
 */
private fun Project.multiplatformSourceSets() =
    extensions.findByType(KotlinMultiplatformExtension::class.java)?.sourceSets
        ?: extensions.findByType(
            org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension::class.java
        )?.sourceSets

/**
 * Disables the legacy test tasks.
 *
 * @param legacyTaskNames The names of the tasks to disable.
 * @since 3.0.0
 */
private fun Project.disableLegacyTestTasks(legacyTaskNames: Set<String>) {
    tasks.withType<AbstractTestTask>()
        .matching { it.name in legacyTaskNames }
        .configureEach { enabled = false }
}

/**
 * Removes the legacy test tasks from `check`.
 *
 * Best effort on multiplatform projects: `check` depends on the Kotlin plugin's aggregate test
 * task, whose children cannot be unregistered. Disabling still makes those tasks skip, so the
 * outcome is right even where the edge survives.
 *
 * @param legacyTaskNames The names of the tasks to remove.
 * @since 3.0.0
 */
private fun Project.removeFromCheck(legacyTaskNames: Set<String>) {
    tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure {
        setDependsOn(dependsOn.filterNot { it.referencesAnyTask(legacyTaskNames) })
    }
}

/**
 * Reports whether a `dependsOn` entry points at one of the given task names.
 *
 * @param names The task names to match.
 * @return `true` when this entry refers to one of them.
 * @since 3.0.0
 */
private fun Any.referencesAnyTask(names: Set<String>): Boolean = when (this) {
    is TaskProvider<*> -> name in names
    is Task -> name in names
    is CharSequence -> toString().removePrefix(":") in names
    else -> false
}

/**
 * Fails the build while the legacy source directories still hold sources.
 *
 * @param legacySourceSets The Kotlin source sets the policy governs, by source set name.
 * @throws GradleException When any legacy directory contains a source file.
 * @since 3.0.0
 */
private fun Project.failOnLegacySources(legacySourceSets: Map<String, KotlinSourceSet>) {
    val populated = legacySourceSets.values
        .flatMap { it.kotlin.srcDirs }
        .filter { it.sourceFileCount() > 0 }
        .sortedBy { it.path }

    if (populated.isEmpty()) return

    // `invariantSeparatorsPath` rather than `path`: the rest of this message spells directories
    // with forward slashes, and `File.path` would print the listing with backslashes on Windows -
    // one message, two conventions, and a reader left wondering whether the difference means
    // something.
    val listing = populated.joinToString("\n") { directory ->
        val relative = directory.relativeToOrSelf(projectDir).invariantSeparatorsPath
        "  - $relative (${directory.sourceFileCount()} files)"
    }

    throw GradleException(
        """
            Named test suites are enabled, and project '$path' still has sources under:

            $listing

            The conventional test source set is not built, so those tests would stop running
            without anything failing. Move them to src/${KreateTasks.Tests.UNIT}/kotlin or
            src/${KreateTasks.Tests.INTEGRATION}/kotlin, or pick another policy:

                kreate { project { tests { legacyTestSourceSet = LegacyTestPolicy.ALIAS } } }

            ALIAS keeps the files where they are and runs them as the unit suite; KEEP leaves
            the conventional test source set running beside the suites.
        """.trimIndent()
    )
}

/**
 * Counts the source files in a directory tree.
 *
 * @return The number of Kotlin and Java source files, or `0` when the directory is absent.
 * @since 3.0.0
 */
private fun File.sourceFileCount(): Int {
    if (!isDirectory) return 0
    return walkTopDown().count { it.isFile && it.extension in SOURCE_EXTENSIONS }
}
