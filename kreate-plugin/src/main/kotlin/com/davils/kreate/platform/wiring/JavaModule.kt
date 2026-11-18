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
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

private const val AUTOMATIC_MODULE_NAME: String = "Automatic-Module-Name"

private const val MAIN_JAVA: String = "MainJava"

private val JVM_PLUGIN_LAYOUT: JavaModuleLayout = JavaModuleLayout(
    descriptor = "src/main/java/module-info.java",
    compileKotlin = "compileKotlin",
    compileJava = "compileJava",
    jar = "jar"
)

private val MODULE_NAME: Regex = Regex("""^[\p{L}_$][\p{L}\p{N}_$]*(\.[\p{L}_$][\p{L}\p{N}_$]*)*$""")

internal fun Project.configureJavaModule(extension: KreateExtension) {
    val moduleName = validModuleName(extension) ?: return
    configureJavaModule(moduleName, JVM_PLUGIN_LAYOUT)
}

internal fun Project.configureTargetJavaModules(extension: KreateExtension) {
    val moduleName = validModuleName(extension) ?: return
    val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
    val jvmTargets = kotlin.targets.withType<KotlinJvmTarget>()
    jvmTargets.forEach { target -> configureJavaModule(moduleName, target.javaModuleLayout()) }
}

private fun KotlinJvmTarget.javaModuleLayout(): JavaModuleLayout {
    val main = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
    val classifier = name.replaceFirstChar(Char::uppercaseChar)
    return JavaModuleLayout(
        descriptor = "src/${main.defaultSourceSet.name}/java/module-info.java",
        compileKotlin = main.compileKotlinTaskName,
        compileJava = "compile$classifier$MAIN_JAVA",
        jar = artifactsTaskName
    )
}

private fun validModuleName(extension: KreateExtension): String? {
    val moduleName = extension.platform.jvm.moduleName.orNull ?: return null
    if (MODULE_NAME.matches(moduleName)) return moduleName

    throw InvalidUserDataException("'$moduleName' is no Java module name such as com.example.library.")
}

private fun Project.configureJavaModule(moduleName: String, layout: JavaModuleLayout) {
    val descriptor = this.layout.projectDirectory.file(layout.descriptor)
    if (descriptor.asFile.isFile) return patchModule(moduleName, layout)

    val jars = tasks.withType<Jar>().matching { jar -> jar.name == layout.jar }
    jars.configureEach { manifest.attributes(mapOf(AUTOMATIC_MODULE_NAME to moduleName)) }
}

private fun Project.patchModule(moduleName: String, layout: JavaModuleLayout) {
    val kotlinCompile = tasks.named<KotlinJvmCompile>(layout.compileKotlin)
    val kotlinClasses = kotlinCompile.flatMap { compile -> compile.destinationDirectory }

    val javaCompilations = tasks.withType<JavaCompile>().matching { compile -> compile.name == layout.compileJava }
    javaCompilations.configureEach { options.compilerArgumentProviders.add(ModulePatch(moduleName, kotlinClasses)) }
}
