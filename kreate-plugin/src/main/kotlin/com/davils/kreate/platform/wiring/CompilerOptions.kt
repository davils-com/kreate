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

package com.davils.kreate.platform.wiring

import com.davils.kreate.KreateExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

private const val EXPECT_ACTUAL_CLASSES: String = "-Xexpect-actual-classes"

internal fun Project.configureCompilerOptions(extension: KreateExtension) {
    val compiler = extension.platform.compiler
    val isProgressive = compiler.progressiveMode.get()
    val reportsExtraWarnings = compiler.extraWarnings.get()
    val languageVersion = compiler.languageVersion.orNull
    val apiVersion = compiler.apiVersion.orNull
    val jvmDefault = compiler.jvmDefault.orNull
    val acceptsExpectActualClasses = compiler.expectActualClasses.get()

    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        val options = compilerOptions
        if (isProgressive) options.progressiveMode.set(true)
        if (reportsExtraWarnings) options.extraWarnings.set(true)
        languageVersion?.let { version -> options.languageVersion.set(version) }
        apiVersion?.let { version -> options.apiVersion.set(version) }
        if (acceptsExpectActualClasses) options.freeCompilerArgs.add(EXPECT_ACTUAL_CLASSES)
    }

    if (jvmDefault == null) return
    tasks.withType<KotlinJvmCompile>().configureEach { compilerOptions.jvmDefault.set(jvmDefault) }
}
