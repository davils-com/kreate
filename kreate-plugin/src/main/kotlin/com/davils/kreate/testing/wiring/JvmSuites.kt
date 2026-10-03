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

internal fun Project.createJvmSuiteSourceSet(suite: TestSuiteExtension) {
    val sourceSetName = suite.sourceSetName.get()
    extensions.getByType<SourceSetContainer>().maybeCreate(sourceSetName)

    extensions.configure<KotlinJvmProjectExtension> {
        applySourceDirectories(sourceSets.getByName(sourceSetName), suite)
    }
}

internal fun Project.wireJvmSuite(suite: TestSuiteExtension): TaskProvider<Test> {
    val sourceSetName = suite.sourceSetName.get()

    extensions.configure<KotlinJvmProjectExtension> {
        associateSuiteCompilation(suite, sourceSetName)
    }

    return tasks.register<Test>(suite.name) {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        val sourceSet = project.extensions.getByType<SourceSetContainer>().getByName(sourceSetName)
        testClassesDirs = sourceSet.output.classesDirs
        classpath = sourceSet.runtimeClasspath
    }
}

internal fun applySourceDirectories(sourceSet: KotlinSourceSet, suite: TestSuiteExtension) {
    val directories = suite.srcDirs.get()
    if (directories.isEmpty()) return
    sourceSet.kotlin.setSrcDirs(directories)
}

private fun KotlinJvmProjectExtension.associateSuiteCompilation(
    suite: TestSuiteExtension,
    sourceSetName: String
) {
    val compilations = target.compilations
    val compilation = compilations.getByName(sourceSetName)

    if (suite.associateWithMain.get()) {
        compilation.associateWith(compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME))
    }

    suite.dependsOnSuites.get().forEach { other ->
        compilation.associateWith(compilations.getByName(other))
    }
}
