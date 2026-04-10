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

public abstract class GenerateBuildConstantsTask : Task("Generates build constants as a Kotlin file.") {
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
    override fun execute() {
        val props = properties.get()
        if (props.isEmpty()) {
            logger.warn("No properties found for build constants.")
        }

        val fileSpec = buildFileSpec(props)
        writeFileSpec(fileSpec)
    }

    private fun buildFileSpec(props: Map<String, String>): FileSpec {
        val isExplicitApi = explicitApi.get()
        val objectSpec = buildObjectSpec(props, isExplicitApi)

        return FileSpec.builder(packageName.get(), className.get())
            .addFileComment(buildFileHeader())
            .addType(objectSpec)
            .build()
    }

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

    private fun buildConstantProperty(key: String, value: String, isExplicitApi: Boolean): PropertySpec {
        return PropertySpec.builder(key.uppercase(), String::class)
            .addModifiers(KModifier.CONST)
            .apply {
                if (isExplicitApi) addModifiers(KModifier.PUBLIC)
            }
            .initializer("%S", value)
            .build()
    }

    private fun buildFileHeader(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        return "This file is generated automatically.\n" +
            "Do not edit or modify! Changes will be overwritten on the next build.\n" +
            "Generated on $timestamp."
    }

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

internal fun Project.registerBuildConstantsTask(extension: KreateExtension) {
    val buildConstants = extension.project.buildConstants
    val platformExtension = extension.platform

    val constantsPath = buildConstants.path.get()
    val className = buildConstants.className.get()
    val resolvedPackageName = resolvePackageName(extension)

    val packagePath = resolvedPackageName.replace('.', '/')
    val outputFile = layout.buildDirectory.file("$constantsPath/$packagePath/$className.kt")

    addBuildConstantsToSourceSets(layout.buildDirectory.dir(constantsPath).get().asFile.absolutePath)

    val task = tasks.register("generateBuildConstants", GenerateBuildConstantsTask::class.java) {
        properties.set(buildConstants.getConstants())
        packageName.set(resolvedPackageName)
        this.className.set(className)
        explicitApi.set(platformExtension.explicitApi)
        file.set(outputFile)
    }

    executeTaskBeforeCompile(task.get())
}

private fun Project.resolvePackageName(extension: KreateExtension): String {
    val buildConstants = extension.project.buildConstants

    if (buildConstants.packageNameOverride.isPresent) {
        return "${buildConstants.packageNameOverride.get()}.build"
    }

    val resolvedName = if (extension.project.name.isPresent) extension.project.name.get() else project.name
    return "${group}.${resolvedName}.build".lowercase().replace(" ", "")
}
