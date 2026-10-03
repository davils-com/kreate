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

import com.davils.kreate.testing.LegacySourceDirectories
import com.davils.kreate.testing.LegacyTestPolicy
import com.davils.kreate.testing.TestSuiteExtension
import com.davils.kreate.testing.TestSuiteNames
import com.davils.kreate.testing.TestsExtension
import com.davils.kreate.testing.suite.enabledSuites
import com.davils.kreate.testing.suite.lowerCamelCaseName
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

private const val LEGACY_SOURCE_SET_SUFFIX: String = "Test"

private val SOURCE_EXTENSIONS: Set<String> = setOf("kt", "kts", "java")

internal fun Project.applyLegacyTestPolicy(
    tests: TestsExtension,
    unitSuiteTask: TaskProvider<out Task>?,
    legacySourceSets: Map<String, KotlinSourceSet>,
    legacyTaskNames: Set<String>
) {
    when (tests.legacyTestSourceSet.get()) {
        LegacyTestPolicy.FAIL -> failOnLegacySources(legacySourceSets)
        LegacyTestPolicy.DISABLE -> retireLegacyTestTasks(legacyTaskNames)
        LegacyTestPolicy.ALIAS -> aliasLegacyTestTasks(legacyTaskNames, unitSuiteTask)
        LegacyTestPolicy.KEEP -> Unit
    }

    applyLegacySourceDirectories(tests, legacySourceSets)
}

private fun Project.retireLegacyTestTasks(legacyTaskNames: Set<String>) {
    disableLegacyTestTasks(legacyTaskNames)
    removeFromCheck(legacyTaskNames)
}

private fun Project.aliasLegacyTestTasks(
    legacyTaskNames: Set<String>,
    unitSuiteTask: TaskProvider<out Task>?
) {
    disableLegacyTestTasks(legacyTaskNames)
    val unit = unitSuiteTask ?: return

    legacyTaskNames.forEach { name ->
        tasks.matching { it.name == name }.configureEach { dependsOn(unit) }
    }
}

private fun Project.applyLegacySourceDirectories(
    tests: TestsExtension,
    legacySourceSets: Map<String, KotlinSourceSet>
) {
    val mode = tests.legacySourceDirectories.get()
    if (mode == LegacySourceDirectories.KEEP) return

    val unitSuite = tests.enabledSuites().firstOrNull { it.name == TestSuiteNames.UNIT }
    val adoptingSuite = unitSuite?.takeIf { mode == LegacySourceDirectories.ADOPT }

    legacySourceSets.forEach { (legacyName, legacySourceSet) ->
        adoptingSuite?.let { suite -> adoptInto(suite, legacyName, legacySourceSet) }
        legacySourceSet.clearSourceDirectories()
    }
}

private fun KotlinSourceSet.clearSourceDirectories() {
    kotlin.setSrcDirs(emptyList<File>())
    resources.setSrcDirs(emptyList<File>())
}

private fun Project.adoptInto(
    unitSuite: TestSuiteExtension,
    legacyName: String,
    legacySourceSet: KotlinSourceSet
) {
    val suiteSourceSetName = unitSuite.sourceSetName.get()
    val targetName = adoptionTargetName(legacyName, suiteSourceSetName)

    val sourceSets = kotlinSourceSets() ?: return
    val destination = sourceSets.findByName(targetName)
        ?: sourceSets.findByName(suiteSourceSetName)
        ?: return

    destination.kotlin.srcDirs(legacySourceSet.kotlin.srcDirs)
    destination.resources.srcDirs(legacySourceSet.resources.srcDirs)
}

private fun adoptionTargetName(legacyName: String, suiteSourceSetName: String): String {
    if (legacyName == TestSuiteNames.LEGACY) return suiteSourceSetName

    val targetPrefix = legacyName.removeSuffix(LEGACY_SOURCE_SET_SUFFIX)
    return lowerCamelCaseName(targetPrefix, suiteSourceSetName)
}

private fun Project.kotlinSourceSets(): NamedDomainObjectContainer<KotlinSourceSet>? {
    val multiplatform = extensions.findByType(KotlinMultiplatformExtension::class.java)
    if (multiplatform != null) return multiplatform.sourceSets

    val jvm = extensions.findByType(KotlinJvmProjectExtension::class.java)
    return jvm?.sourceSets
}

private fun Project.disableLegacyTestTasks(legacyTaskNames: Set<String>) {
    val legacyTestTasks = tasks.withType<AbstractTestTask>().matching { it.name in legacyTaskNames }
    legacyTestTasks.configureEach { enabled = false }
}

private fun Project.removeFromCheck(legacyTaskNames: Set<String>) {
    tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure {
        setDependsOn(dependsOn.filterNot { it.referencesAnyTask(legacyTaskNames) })
    }
}

private fun Any.referencesAnyTask(names: Set<String>): Boolean {
    val taskName = referencedTaskName() ?: return false
    return taskName in names
}

private fun Any.referencedTaskName(): String? {
    if (this is TaskProvider<*>) return name
    if (this is Task) return name
    if (this is CharSequence) return toString().removePrefix(Project.PATH_SEPARATOR)
    return null
}

private fun Project.failOnLegacySources(legacySourceSets: Map<String, KotlinSourceSet>) {
    val legacyDirectories = legacySourceSets.values.flatMap { it.kotlin.srcDirs }
    val populated = legacyDirectories.filter { it.sourceFileCount() > 0 }.sortedBy { it.path }
    if (populated.isEmpty()) return

    val listing = populated.joinToString("\n") { directory ->
        val relative = directory.forwardSlashPathFrom(projectDir)
        "  - $relative (${directory.sourceFileCount()} files)"
    }

    throw GradleException(
        """
            Named test suites are enabled, and project '$path' still has sources under:

            $listing

            The conventional test source set is not built, so those tests would stop running
            without anything failing. Move them to src/${TestSuiteNames.UNIT}/kotlin or
            src/${TestSuiteNames.INTEGRATION}/kotlin, or pick another policy:

                kreate { project { tests { legacyTestSourceSet = LegacyTestPolicy.ALIAS } } }

            ALIAS keeps the files where they are and runs them as the unit suite; KEEP leaves
            the conventional test source set running beside the suites.
        """.trimIndent()
    )
}

private fun File.forwardSlashPathFrom(baseDirectory: File): String =
    relativeToOrSelf(baseDirectory).invariantSeparatorsPath

private fun File.sourceFileCount(): Int {
    if (!isDirectory) return 0
    return walkTopDown().count { it.isFile && it.extension in SOURCE_EXTENSIONS }
}
