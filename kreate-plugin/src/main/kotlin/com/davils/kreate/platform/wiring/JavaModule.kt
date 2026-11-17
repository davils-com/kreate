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
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

private const val MODULE_DESCRIPTOR: String = "src/main/java/module-info.java"

private const val AUTOMATIC_MODULE_NAME: String = "Automatic-Module-Name"

private const val COMPILE_JAVA: String = "compileJava"

private const val COMPILE_KOTLIN: String = "compileKotlin"

private const val JAR: String = "jar"

private val MODULE_NAME: Regex = Regex("""^[\p{L}_$][\p{L}\p{N}_$]*(\.[\p{L}_$][\p{L}\p{N}_$]*)*$""")

internal fun Project.configureJavaModule(extension: KreateExtension) {
    val moduleName = extension.platform.jvm.moduleName.orNull ?: return
    if (!MODULE_NAME.matches(moduleName)) {
        throw InvalidUserDataException("'$moduleName' is no Java module name such as com.example.library.")
    }
    val descriptor = layout.projectDirectory.file(MODULE_DESCRIPTOR)
    if (descriptor.asFile.isFile) return patchModule(moduleName)

    tasks.named<Jar>(JAR) {
        manifest.attributes(mapOf(AUTOMATIC_MODULE_NAME to moduleName))
    }
}

private fun Project.patchModule(moduleName: String) {
    val kotlinCompile = tasks.named<KotlinJvmCompile>(COMPILE_KOTLIN)
    val kotlinClasses = kotlinCompile.flatMap { compile -> compile.destinationDirectory }

    tasks.named<JavaCompile>(COMPILE_JAVA) {
        options.compilerArgumentProviders.add(ModulePatch(moduleName, kotlinClasses))
    }
}
