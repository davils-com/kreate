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

import com.davils.kreate.testing.TestSuiteExtension
import com.davils.kreate.testing.suite.sharedSourceSetName
import com.davils.kreate.testing.suite.targetTestTaskName
import com.davils.kreate.testing.task.SuiteJvmTest
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyTemplate
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.extend
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

private const val NO_JVM_TARGETS: String = "<none declared>"

private const val NO_TARGETS: String = "<no targets>"

@OptIn(ExperimentalKotlinGradlePluginApi::class)
internal fun KotlinMultiplatformExtension.applySuiteHierarchyTemplate(
    suites: List<TestSuiteExtension>
) {
    val trees = suites
        .map { suite -> KotlinSourceSetTree(suite.sourceSetName.get()) }
        .toTypedArray()
    if (trees.isEmpty()) return

    applyHierarchyTemplate(
        KotlinHierarchyTemplate.default.extend { withSourceSetTree(*trees) }
    )
}

internal fun Project.createKmpSuiteSourceSets(
    suite: TestSuiteExtension,
    kotlin: KotlinMultiplatformExtension
) {
    val sourceSetName = suite.sourceSetName.get()
    val shared = kotlin.sourceSets.maybeCreate(sharedSourceSetName(sourceSetName))
    applySourceDirectories(shared, suite)

    rejectUnsupportedTargets(suite, kotlin, suite.targets.get())
    val targets = suite.applicableTargets(kotlin)
    if (targets.isEmpty()) throw noApplicableTargetFailure(suite, kotlin)

    targets.forEach { target -> target.compilations.maybeCreate(sourceSetName) }
}

private fun TestSuiteExtension.applicableTargets(
    kotlin: KotlinMultiplatformExtension
): List<KotlinJvmTarget> {
    val requested = targets.get()
    val takesEveryTarget = requested.isEmpty()
    return kotlin.jvmTargets().filter { takesEveryTarget || it.name in requested }
}

internal fun Project.wireMultiplatformSuite(
    suite: TestSuiteExtension,
    kotlin: KotlinMultiplatformExtension
): List<TaskProvider<SuiteJvmTest>> {
    val sourceSetName = suite.sourceSetName.get()

    return suite.applicableTargets(kotlin).map { target ->
        val compilation = target.compilations.getByName(sourceSetName)
        associateSuiteCompilation(suite, target, compilation)
        registerSuiteJvmTest(suite, target, compilation)
    }
}

private fun associateSuiteCompilation(
    suite: TestSuiteExtension,
    target: KotlinJvmTarget,
    compilation: KotlinJvmCompilation
) {
    val compilations = target.compilations

    if (suite.associateWithMain.get()) {
        compilation.associateWith(compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME))
    }

    suite.dependsOnSuites.get().forEach { other ->
        compilation.associateWith(compilations.getByName(other))
    }
}

private fun Project.registerSuiteJvmTest(
    suite: TestSuiteExtension,
    target: KotlinJvmTarget,
    compilation: KotlinJvmCompilation
): TaskProvider<SuiteJvmTest> {
    val taskName = targetTestTaskName(target.disambiguationClassifier, suite.sourceSetName.get())
    val task = tasks.register(taskName, SuiteJvmTest::class.java, target.targetName)

    task.configure {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "${suite.description.get()} (${target.name})"
        testClassesDirs = files(compilation.output.classesDirs)
        classpath = files(compilation.output.allOutputs, compilation.runtimeDependencyFiles)
    }

    return task
}

private fun KotlinMultiplatformExtension.jvmTargets(): List<KotlinJvmTarget> =
    targets.withType<KotlinJvmTarget>().toList()

private fun rejectUnsupportedTargets(
    suite: TestSuiteExtension,
    kotlin: KotlinMultiplatformExtension,
    requested: List<String>
) {
    if (requested.isEmpty()) return
    val jvmTargets = kotlin.jvmTargets()
    val jvmTargetNames = jvmTargets.map { it.name }.toSet()
    val unsupported = requested.filterNot { it in jvmTargetNames }
    if (unsupported.isEmpty()) return

    val sortedJvmTargetNames = jvmTargetNames.sorted()
    val supportedTargets = sortedJvmTargetNames.joinToString().ifEmpty { NO_JVM_TARGETS }

    throw GradleException(
        """
            Test suite '${suite.name}' requests the target(s) ${unsupported.joinToString()}, which
            cannot carry a named test suite.

            Only JVM targets can. The Kotlin plugin binds the test binary of a Native target and
            the test run of a JS or Wasm target to the compilation called 'test', and offers no
            way to point them at another one - so the suite would give you a source directory
            that is never compiled and never run.

            Use one of $supportedTargets,
            or leave `targets` empty to use every JVM target. Tests for the other targets stay
            in their conventional test source sets; set
            `kreate { project { tests { legacyTestSourceSet = LegacyTestPolicy.KEEP } } }` to
            keep those running.
        """.trimIndent()
    )
}

private fun Project.noApplicableTargetFailure(
    suite: TestSuiteExtension,
    kotlin: KotlinMultiplatformExtension
): GradleException {
    val sortedTargetNames = kotlin.targets.map { it.name }.sorted()
    val declaredTargets = sortedTargetNames.joinToString().ifEmpty { NO_TARGETS }

    return GradleException(
        """
            Test suite '${suite.name}' in project '$path' applies to no target.

            Named test suites need a JVM target, and this project declares none:
            $declaredTargets.

            Add a `jvm()` target, or disable the suite with
            `kreate { project { tests { suites { named("${suite.name}") { enabled = false } } } } }`.
        """.trimIndent()
    )
}
