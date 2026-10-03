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

package com.davils.kreate.constants.task

import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss"
private const val GENERATED_KDOC = "Auto-generated build constants."
private const val STRING_LITERAL_FORMAT = "%S"

@DisableCachingByDefault(because = "Generated constants include a timestamp and depend on project properties")
internal abstract class GenerateBuildConstants : KreateTask(
    "Generates build constants as a Kotlin file.",
    KreateTaskGroup.BUILD_CONSTANTS
) {
    @get:Input
    public abstract val properties: MapProperty<String, String>

    @get:Input
    public abstract val packageName: Property<String>

    @get:Input
    public abstract val className: Property<String>

    @get:Input
    public abstract val explicitApi: Property<Boolean>

    @get:OutputFile
    public abstract val file: RegularFileProperty

    @TaskAction
    public fun execute() {
        val props = properties.get()
        if (props.isEmpty()) {
            logger.warn("No properties found for build constants.")
        }

        val fileSpec = buildFileSpec(props)
        writeFileSpec(fileSpec)
    }

    private fun buildFileSpec(props: Map<String, String>): FileSpec {
        val objectSpec = buildObjectSpec(props, explicitApi.get())

        val builder = FileSpec.builder(packageName = packageName.get(), fileName = className.get())
        builder.addFileComment(buildFileHeader())
        builder.addType(objectSpec)
        return builder.build()
    }

    private fun buildObjectSpec(props: Map<String, String>, isExplicitApi: Boolean): TypeSpec {
        val builder = TypeSpec.objectBuilder(className.get())
        builder.addKdoc(GENERATED_KDOC)
        if (isExplicitApi) builder.addModifiers(KModifier.PUBLIC)

        props.forEach { (key, value) ->
            builder.addProperty(buildConstantProperty(key, value, isExplicitApi))
        }

        return builder.build()
    }

    private fun buildConstantProperty(key: String, value: String, isExplicitApi: Boolean): PropertySpec {
        val builder = PropertySpec.builder(key.uppercase(), String::class)
        builder.addModifiers(KModifier.CONST)
        if (isExplicitApi) builder.addModifiers(KModifier.PUBLIC)
        builder.initializer(STRING_LITERAL_FORMAT, value)
        return builder.build()
    }

    private fun buildFileHeader(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN))
        return "This file is generated automatically.\n" +
            "Do not edit or modify! Changes will be overwritten on the next build.\n" +
            "Generated on $timestamp."
    }

    private fun writeFileSpec(fileSpec: FileSpec) {
        val outputFile = file.get().asFile
        try {
            outputFile.parentFile.mkdirs()
            val raw = fileSpec.toString()
            val content = raw.replaceFirst(
                oldValue = "package ${fileSpec.packageName}", newValue = "\npackage ${fileSpec.packageName}"
            )
            outputFile.writeText(content)
            logger.lifecycle("Wrote build constants file to ${outputFile.absolutePath}.")
        } catch (e: IOException) {
            logger.error("Failed to write build constants file.", e)
        }
    }
}
