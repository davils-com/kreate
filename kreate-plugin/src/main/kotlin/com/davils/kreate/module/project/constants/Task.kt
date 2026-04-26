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

package com.davils.kreate.module.project.constants

import com.davils.kreate.KreateExtension
import com.davils.kreate.jobs.Task
import com.davils.kreate.jobs.executeTaskBeforeCompile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Task to generate build constants as a Kotlin source file.
 *
 * This task uses KotlinPoet to create an object containing constants defined
 * in the [BuildConstantsExtension]. The generated file is placed in the
 * specified output directory and included in the project's source sets.
 *
 * @since 1.0.0
 */
public abstract class GenerateBuildConstantsTask : Task("Generates build constants as a Kotlin file.", "kreate build-constants") {
    /**
     * The map of properties to generate as constants.
     * @since 1.0.0
     */
    @get:Input
    public abstract val properties: MapProperty<String, String>

    /**
     * The package name for the generated class.
     * @since 1.0.0
     */
    @get:Input
    public abstract val packageName: Property<String>

    /**
     * The name of the generated class.
     * @since 1.0.0
     */
    @get:Input
    public abstract val className: Property<String>

    /**
     * Whether to use explicit API mode for the generated class.
     * @since 1.0.0
     */
    @get:Input
    public abstract val explicitApi: Property<Boolean>

    /**
     * The output file where the generated code will be written.
     * @since 1.0.0
     */
    @get:OutputFile
    public abstract val file: RegularFileProperty

    /**
     * Executes the task to generate the build constants file.
     *
     * @since 1.0.0
     */
    @TaskAction
    override fun execute() {
        val props = properties.get()
        if (props.isEmpty()) {
            logger.warn("No properties found for build constants.")
        }

        val fileSpec = buildFileSpec(props)
        writeFileSpec(fileSpec)
    }

    /**
     * Builds the [FileSpec] for the generated file.
     *
     * @param props The map of constants.
     * @return The constructed [FileSpec].
     * @since 1.0.0
     */
    private fun buildFileSpec(props: Map<String, String>): FileSpec {
        val isExplicitApi = explicitApi.get()
        val objectSpec = buildObjectSpec(props, isExplicitApi)

        return FileSpec.builder(packageName.get(), className.get())
            .addFileComment(buildFileHeader())
            .addType(objectSpec)
            .build()
    }

    /**
     * Builds the [TypeSpec] for the generated object.
     *
     * @param props The map of constants.
     * @param isExplicitApi Whether to use explicit public modifiers.
     * @return The constructed [TypeSpec].
     * @since 1.0.0
     */
    private fun buildObjectSpec(props: Map<String, String>, isExplicitApi: Boolean): TypeSpec {
        val builder = TypeSpec.objectBuilder(className.get())
            .addKdoc("Auto-generated build constants.")
            .apply {
                if (isExplicitApi) addModifiers(KModifier.PUBLIC)
            }

        props.forEach { (key, value) ->
            builder.addProperty(buildConstantProperty(key, value, isExplicitApi))
        }

        return builder.build()
    }

    /**
     * Builds a [PropertySpec] for a single constant.
     *
     * @param key The name of the constant.
     * @param value The value of the constant.
     * @param isExplicitApi Whether to use explicit public modifiers.
     * @return The constructed [PropertySpec].
     * @since 1.0.0
     */
    private fun buildConstantProperty(key: String, value: String, isExplicitApi: Boolean): PropertySpec {
        return PropertySpec.builder(key.uppercase(), String::class)
            .addModifiers(KModifier.CONST)
            .apply {
                if (isExplicitApi) addModifiers(KModifier.PUBLIC)
            }
            .initializer("%S", value)
            .build()
    }

    /**
     * Builds the header comment for the generated file.
     *
     * @return The header string.
     * @since 1.0.0
     */
    private fun buildFileHeader(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        return "This file is generated automatically.\n" +
            "Do not edit or modify! Changes will be overwritten on the next build.\n" +
            "Generated on $timestamp."
    }

    /**
     * Writes the [FileSpec] to the output file.
     *
     * @param fileSpec The file specification to write.
     * @since 1.0.0
     */
    private fun writeFileSpec(fileSpec: FileSpec) {
        val outputFile = file.get().asFile
        try {
            outputFile.parentFile.mkdirs()
            val raw = fileSpec.toString()
            val content = raw.replaceFirst("package ${fileSpec.packageName}", "\npackage ${fileSpec.packageName}")
            outputFile.writeText(content)
            logger.lifecycle("Wrote build constants file to ${outputFile.absolutePath}.")
        } catch (e: Exception) {
            logger.error("Failed to write build constants file.", e)
        }
    }
}

/**
 * Registers the build constants generation task for the project.
 *
 * @param extension The Kreate configuration extension.
 * @since 1.0.0
 */
internal fun Project.registerBuildConstantsTask(extension: KreateExtension) {
    val buildConstants = extension.project.buildConstants
    val platformExtension = extension.platform

    val constantsPath = buildConstants.path.get()
    val className = buildConstants.className.get()
    val resolvedPackageName = resolvePackageName(extension)

    val packagePath = resolvedPackageName.replace('.', '/')
    val outputFile = layout.buildDirectory.file("$constantsPath/$packagePath/$className.kt")

    addBuildConstantsToSourceSets(layout.buildDirectory.dir(constantsPath).get().asFile.absolutePath)

    val task = tasks.register("kreate-build-constants", GenerateBuildConstantsTask::class.java) {
        properties.set(buildConstants.getConstants())
        packageName.set(resolvedPackageName)
        this.className.set(className)
        explicitApi.set(platformExtension.explicitApi)
        file.set(outputFile)
    }

    executeTaskBeforeCompile(task.get())
}

/**
 * Resolves the package name for the generated build constants.
 *
 * @param extension The Kreate configuration extension.
 * @return The resolved package name.
 * @since 1.0.0
 */
private fun Project.resolvePackageName(extension: KreateExtension): String {
    val buildConstants = extension.project.buildConstants

    if (buildConstants.packageNameOverride.isPresent) {
        return "${buildConstants.packageNameOverride.get()}.build"
    }

    val resolvedName = if (extension.project.name.isPresent) extension.project.name.get() else project.name
    return "${group}.${resolvedName}.build".lowercase().replace(" ", "")
}
