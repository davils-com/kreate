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

package com.davils.kreate.benchmark.wiring

import com.davils.kreate.benchmark.BenchmarkExtension
import com.davils.kreate.gradle.GradlePluginId
import com.davils.kreate.gradle.KotlinPluginId
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension
import org.jetbrains.kotlin.allopen.gradle.AllOpenGradleSubplugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

private const val JMH_STATE_ANNOTATION = "org.openjdk.jmh.annotations.State"
private const val BENCHMARK_RUNTIME_MODULE = "org.jetbrains.kotlinx:kotlinx-benchmark-runtime"

internal fun Project.setUpBenchmarkSourceSet(extension: BenchmarkExtension): String {
    val name = extension.sourceSetName.get()

    if (extension.applyAllOpen.get()) applyAllOpenForJmh()
    if (!extension.createSourceSet.get()) return name

    val runtime = "$BENCHMARK_RUNTIME_MODULE:${extension.runtimeVersion.get()}"
    plugins.withId(GradlePluginId.JAVA) { addJvmBenchmarkSourceSet(name, runtime) }
    plugins.withId(KotlinPluginId.MULTIPLATFORM) { addMultiplatformBenchmarks(name, runtime) }

    return name
}

private fun Project.addJvmBenchmarkSourceSet(name: String, runtime: String) {
    extensions.getByType<SourceSetContainer>().maybeCreate(name)
    dependencies.add("${name}Implementation", runtime)

    plugins.withId(KotlinPluginId.JVM) { associateJvmBenchmarks(name) }
}

private fun Project.associateJvmBenchmarks(name: String) {
    extensions.configure<KotlinJvmProjectExtension> {
        val compilations = target.compilations
        val benchmarkCompilation = compilations.getByName(name)
        benchmarkCompilation.associateWith(compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME))
    }
}

private fun Project.addMultiplatformBenchmarks(name: String, runtime: String) {
    extensions.configure<KotlinMultiplatformExtension> {
        targets.withType<KotlinJvmTarget>().configureEach {
            val benchmarkCompilation = compilations.maybeCreate(name)
            benchmarkCompilation.associateWith(compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME))
            dependencies.add(benchmarkCompilation.compileDependencyConfigurationName, runtime)
        }
    }
}

private fun Project.applyAllOpenForJmh() {
    pluginManager.apply(AllOpenGradleSubplugin::class)
    extensions.configure<AllOpenExtension> { annotation(JMH_STATE_ANNOTATION) }
}
