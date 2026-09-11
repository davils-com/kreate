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

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * Creates the source set of one suite on a Kotlin/JVM project.
 *
 * Split from [wireJvmSuite] because a suite may associate with another one, and a container
 * hands its elements back in alphabetical order rather than in the order they were registered -
 * so `integrationTest` is configured before the `unitTest` whose fixtures it asked for. Every
 * source set is created first, and only then is anything wired to anything else.
 *
 * The source set is created through Gradle's [SourceSetContainer] rather than through
 * `testing { suites { } }`. Two reasons, and the second is the decisive one. The Kotlin plugin
 * mirrors every Java source set into a Kotlin compilation through a live hook, so
 * `src/<name>/kotlin` and the `<name>Implementation` configurations appear without being
 * asked for. And a `JvmTestSuite` wires itself to the code under test with
 * `implementation(project())`, which is the published view of the project: `internal`
 * declarations stay invisible, and the only way to get them back is the compilation
 * association in [wireJvmSuite] - at which point the production classes are on the classpath
 * twice.
 *
 * @param suite The suite configuration.
 * @since 3.0.0
 */
internal fun Project.createJvmSuiteSourceSet(suite: TestSuiteExtension) {
    val sourceSetName = suite.sourceSetName.get()
    extensions.getByType<SourceSetContainer>().maybeCreate(sourceSetName)

    extensions.configure<KotlinJvmProjectExtension> {
        applySourceDirectories(sourceSets.getByName(sourceSetName), suite)
    }
}

/**
 * Associates one suite's compilation and registers its test task.
 *
 * @param suite The suite configuration.
 * @return The registered test task.
 * @since 3.0.0
 */
internal fun Project.wireJvmSuite(suite: TestSuiteExtension): TaskProvider<Test> {
    val sourceSetName = suite.sourceSetName.get()

    extensions.configure<KotlinJvmProjectExtension> {
        val compilations = target.compilations
        val compilation = compilations.getByName(sourceSetName)

        // What makes `internal` declarations visible to the tests, by way of the friend paths
        // the association sets up, and what puts main's own dependencies on the suite's
        // classpath. A suite without it can only exercise the published surface.
        if (suite.associateWithMain.get()) {
            compilation.associateWith(compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME))
        }

        suite.dependsOnSuites.get().forEach { other ->
            compilation.associateWith(compilations.getByName(other))
        }
    }

    return tasks.register<Test>(suite.name) {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        val sourceSet = project.extensions.getByType<SourceSetContainer>().getByName(sourceSetName)
        testClassesDirs = sourceSet.output.classesDirs
        classpath = sourceSet.runtimeClasspath
    }
}

/**
 * Replaces a source set's Kotlin source directories when the suite configures its own.
 *
 * @param sourceSet The Kotlin source set backing the suite.
 * @param suite The suite configuration.
 * @since 3.0.0
 */
internal fun applySourceDirectories(sourceSet: KotlinSourceSet, suite: TestSuiteExtension) {
    val directories = suite.srcDirs.get()
    if (directories.isEmpty()) return
    sourceSet.kotlin.setSrcDirs(directories)
}
