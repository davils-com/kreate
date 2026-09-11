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
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

/**
 * Extends the Kotlin hierarchy template so that the suites' source set trees are wired up.
 *
 * This is the part of multiplatform support that cannot be skipped. A compilation named
 * `unitTest` is already classified into a `unitTest` source set tree by the Kotlin plugin, but
 * the default template declares itself applicable to the `main` and `test` trees only - so
 * `commonUnitTest` is created, never connected to anything, and reported as an unused source
 * set while the tests in it silently do not run.
 *
 * `dependsOn` would connect it in one line and must not be used: the Kotlin plugin abandons its
 * entire default hierarchy the moment it finds one manually added refines edge anywhere in the
 * project, and `nativeMain`, `appleMain` and the rest stop existing without the build failing.
 * Extending the default template instead adds the missing trees and keeps everything the
 * default already did.
 *
 * Applying a template at all means the plugin no longer applies its own, which is why this
 * extends [KotlinHierarchyTemplate.default] rather than describing a fresh one.
 *
 * @param suites The suites whose trees need wiring.
 * @since 3.0.0
 */
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

/**
 * Creates the shared source set and the per-target compilations of one suite on a Kotlin
 * Multiplatform project.
 *
 * Creating the shared source set here rather than leaving it to the template is what makes the
 * Kotlin plugin's older fallback path work too, because that one only wires `common<Tree>` if
 * the source set already exists by the time it runs.
 *
 * @param suite The suite configuration.
 * @param kotlin The multiplatform extension.
 * @return The per-target test tasks, one for each JVM target the suite applies to.
 * @throws GradleException When the suite names a target that cannot carry a named suite, or
 * applies to no target at all.
 * @since 3.0.0
 */
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

/**
 * Returns the JVM targets one suite applies to.
 *
 * @param kotlin The multiplatform extension.
 * @return The matching JVM targets.
 * @since 3.0.0
 */
internal fun TestSuiteExtension.applicableTargets(
    kotlin: KotlinMultiplatformExtension
): List<KotlinJvmTarget> {
    val requested = targets.get()
    return kotlin.targets.withType<KotlinJvmTarget>()
        .filter { requested.isEmpty() || it.name in requested }
}

/**
 * Associates one suite's compilations and registers its per-target test tasks.
 *
 * @param suite The suite configuration.
 * @param kotlin The multiplatform extension.
 * @return The per-target test tasks, one for each JVM target the suite applies to.
 * @since 3.0.0
 */
internal fun Project.wireMultiplatformSuite(
    suite: TestSuiteExtension,
    kotlin: KotlinMultiplatformExtension
): List<TaskProvider<SuiteJvmTest>> {
    val sourceSetName = suite.sourceSetName.get()

    return suite.applicableTargets(kotlin).map { target ->
        val compilation = target.compilations.getByName(sourceSetName)

        if (suite.associateWithMain.get()) {
            compilation.associateWith(
                target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
            )
        }

        suite.dependsOnSuites.get().forEach { other ->
            compilation.associateWith(target.compilations.getByName(other))
        }

        // A KotlinJvmTest rather than a plain Test, and with `targetName` set: the coverage
        // engine finds multiplatform test tasks by that type and that property, and a plain
        // Test task here would leave the suite out of the measurement while every test still
        // passes.
        val task = tasks.register(
            targetTestTaskName(target.disambiguationClassifier, sourceSetName),
            SuiteJvmTest::class.java,
            target.targetName
        )

        task.configure {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "${suite.description.get()} (${target.name})"
            testClassesDirs = files(compilation.output.classesDirs)
            classpath = files(compilation.output.allOutputs, compilation.runtimeDependencyFiles)
        }

        task
    }
}

/**
 * Fails the build if the suite names a target that cannot carry a named suite.
 *
 * Only JVM targets can. The Kotlin plugin ties the test binary of a Native target and the test
 * run of a JS or Wasm target to the compilation literally called `test`, with no public way to
 * point either at another one. Creating the source set anyway would produce a directory that
 * looks tested and is never compiled, so this fails instead.
 *
 * @param suite The suite configuration.
 * @param kotlin The multiplatform extension.
 * @param requested The target names the suite asked for.
 * @throws GradleException When a requested target is unknown or is not a JVM target.
 * @since 3.0.0
 */
private fun rejectUnsupportedTargets(
    suite: TestSuiteExtension,
    kotlin: KotlinMultiplatformExtension,
    requested: List<String>
) {
    if (requested.isEmpty()) return
    val jvmTargetNames = kotlin.targets.withType<KotlinJvmTarget>().map { it.name }.toSet()
    val unsupported = requested.filterNot { it in jvmTargetNames }
    if (unsupported.isEmpty()) return

    throw GradleException(
        """
            Test suite '${suite.name}' requests the target(s) ${unsupported.joinToString()}, which
            cannot carry a named test suite.

            Only JVM targets can. The Kotlin plugin binds the test binary of a Native target and
            the test run of a JS or Wasm target to the compilation called 'test', and offers no
            way to point them at another one - so the suite would give you a source directory
            that is never compiled and never run.

            Use one of ${jvmTargetNames.sorted().joinToString().ifEmpty { "<none declared>" }},
            or leave `targets` empty to use every JVM target. Tests for the other targets stay
            in their conventional test source sets; set
            `kreate { project { tests { legacyTestSourceSet = LegacyTestPolicy.KEEP } } }` to
            keep those running.
        """.trimIndent()
    )
}

/**
 * Builds the failure raised when a suite applies to no target.
 *
 * @param suite The suite configuration.
 * @param kotlin The multiplatform extension.
 * @return The exception to throw.
 * @since 3.0.0
 */
private fun Project.noApplicableTargetFailure(
    suite: TestSuiteExtension,
    kotlin: KotlinMultiplatformExtension
): GradleException = GradleException(
    """
        Test suite '${suite.name}' in project '$path' applies to no target.

        Named test suites need a JVM target, and this project declares none:
        ${kotlin.targets.map { it.name }.sorted().joinToString().ifEmpty { "<no targets>" }}.

        Add a `jvm()` target, or disable the suite with
        `kreate { project { tests { suites { named("${suite.name}") { enabled = false } } } } }`.
    """.trimIndent()
)
