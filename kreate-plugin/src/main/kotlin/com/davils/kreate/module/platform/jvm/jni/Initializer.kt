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

package com.davils.kreate.module.platform.jvm.jni

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreateTasks
import com.davils.kreate.module.platform.jvm.jni.tasks.BuildNative
import com.davils.kreate.module.platform.jvm.jni.tasks.ConfigureNative
import com.davils.kreate.module.platform.jvm.jni.tasks.GenerateDigestManifest
import com.davils.kreate.module.platform.jvm.jni.tasks.GenerateJniHeaders
import com.davils.kreate.module.platform.jvm.jni.tasks.GenerateNativeLoader
import com.davils.kreate.module.platform.jvm.jni.tasks.InitializeCppProject
import com.davils.kreate.system.currentPlatformId
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import java.io.File

/**
 * Initializes the JNI configuration for the project.
 *
 * Sources live under `<projectDirectory>/<projectName>/src` with a `CMakeLists.txt` at
 * `<projectDirectory>/<projectName>/CMakeLists.txt`, mirroring the C-interop layout. All
 * build outputs, in contrast, live under Gradle's build directory.
 *
 * The pipeline runs *after* Kotlin compilation rather than before it. That ordering is
 * what makes header generation possible at all — the headers are derived from the compiled
 * `external` declarations — and it also removes a native build from the critical path of
 * every Kotlin compile. Nothing on the JVM side needs the shared library until something
 * is actually executed.
 *
 * @param extension The Kreate configuration extension.
 * @return Unit
 * @since 1.1.0
 */
internal fun Project.initializeJni(extension: KreateExtension) {
    val jniConfig = extension.platform.jvm.jni
    if (!jniConfig.enabled.get()) return

    val projectName = resolveProjectName(extension)
    val nativeProjectDir = resolveRootDir(jniConfig).resolve(projectName)
    val platformId = currentPlatformId()

    val cmakeBuildDir = layout.buildDirectory.dir("jni/$platformId/cmake")
    val libraryDir = layout.buildDirectory.dir("jni/$platformId/lib")
    val headerDir = layout.buildDirectory.dir("generated/jni/include")
    val javaHome = resolveToolchainJavaHome(extension)

    val initialize = tasks.register<InitializeCppProject>(KreateTasks.Jni.INITIALIZE) {
        this.projectName.set(projectName)
        this.projectRoot.set(layout.dir(provider { nativeProjectDir }))
    }

    val headers = tasks.register<GenerateJniHeaders>(KreateTasks.Jni.HEADERS) {
        classDirectories.from(jvmMainClassDirectories())
        headerFileName.set(jniConfig.headers.fileName.orElse("${projectName}_jni.h"))
        outputDirectory.set(headerDir)
        onlyIf { jniConfig.headers.enabled.get() }
    }

    val configure = tasks.register<ConfigureNative>(KreateTasks.Jni.CONFIGURE) {
        sourceDirectory.set(layout.dir(provider { nativeProjectDir }))
        cmakeListsFile.set(layout.file(provider { nativeProjectDir.resolve("CMakeLists.txt") }))
        cacheBoundPaths.set(
            cmakeBuildDir.map { listOf(nativeProjectDir.absolutePath, it.asFile.absolutePath) }
        )
        generatedHeaderDirectory.set(headerDir)
        buildType.set(jniConfig.buildType)
        generator.set(jniConfig.generator)
        cmakeExecutable.set(jniConfig.cmakeExecutable)
        this.javaHome.set(javaHome)
        libraryIncludePaths.set(jniConfig.libraryIncludePaths)
        cmakeBuildDirectory.set(cmakeBuildDir)
        libraryOutputDirectory.set(libraryDir)
        cmakeCache.set(cmakeBuildDir.map { it.file("CMakeCache.txt") })
        dependsOn(initialize, headers)
    }

    val build = tasks.register<BuildNative>(KreateTasks.Jni.BUILD) {
        nativeSources.from(
            fileTree(nativeProjectDir) {
                include("CMakeLists.txt", "src/**", "include/**")
            },
            headerDir
        )
        cmakeCache.set(configure.flatMap { it.cmakeCache })
        buildType.set(jniConfig.buildType)
        cmakeExecutable.set(jniConfig.cmakeExecutable)
        this.javaHome.set(javaHome)
        cmakeBuildDirectory.set(cmakeBuildDir)
        libraryOutputDirectory.set(libraryDir)
    }

    applyRuntimeLibraryPath(jniConfig, libraryDir, build.name)
    applyNativePackaging(extension, projectName, libraryDir, platformId, build.name)
}

/**
 * Wires `-Djava.library.path` into every task that runs project code.
 *
 * @param jniConfig The JNI configuration extension.
 * @param libraryDir The directory the native build writes its artifacts to.
 * @param buildTaskName The name of the native build task these tasks must wait for.
 * @return Unit
 * @since 1.1.0
 */
private fun Project.applyRuntimeLibraryPath(
    jniConfig: JniExtension,
    libraryDir: Provider<out org.gradle.api.file.Directory>,
    buildTaskName: String
) {
    val libraryPath = libraryDir.map { directory ->
        val paths = mutableListOf(directory.asFile.absolutePath)
        jniConfig.libraryRuntimePaths.getOrElse(emptyList()).forEach { path ->
            paths += file(path).absolutePath
        }
        paths.joinToString(File.pathSeparator)
    }

    tasks.withType<Test>().configureEach {
        dependsOn(buildTaskName)
        jvmArgumentProviders.add(JavaLibraryPathArgumentProvider(libraryPath))
    }

    tasks.withType<JavaExec>().configureEach {
        dependsOn(buildTaskName)
        jvmArgumentProviders.add(JavaLibraryPathArgumentProvider(libraryPath))
    }
}

/**
 * Packages the built native libraries into the project's JAR and generates the loader.
 *
 * @param extension The Kreate configuration extension.
 * @param projectName The native library base name.
 * @param libraryDir The directory the native build writes its artifacts to.
 * @param platformId The `<os>-<arch>` identifier the libraries are filed under.
 * @param buildTaskName The name of the native build task the JAR must wait for.
 * @return Unit
 * @since 2.0.0
 */
private fun Project.applyNativePackaging(
    extension: KreateExtension,
    projectName: String,
    libraryDir: Provider<out org.gradle.api.file.Directory>,
    platformId: String,
    buildTaskName: String
) {
    val packaging = extension.platform.jvm.jni.packaging
    if (!packaging.enabled.get()) return

    val resourcePath = packaging.resourcePath.get()
    val publishPerPlatform = packaging.publishing.enabled.get()
    val manifest = registerDigestManifest(packaging, libraryDir, platformId, buildTaskName)

    // With per-platform publishing the natives leave the main JAR entirely, so that no consumer
    // silently receives whichever platform the library happened to be built on. They arrive
    // through the platform artifacts registered below instead.
    if (!publishPerPlatform) {
        tasks.withType<Jar>().configureEach {
            dependsOn(buildTaskName)
            from(libraryDir) {
                into("$resourcePath/$platformId")
            }
            if (manifest != null) {
                from(manifest) {
                    into(resourcePath)
                }
            }
        }
    } else {
        configureNativePublishing(
            extension = extension,
            projectName = projectName,
            hostLibraryDir = libraryDir,
            hostPlatformId = platformId,
            resourcePath = resourcePath,
            buildTaskName = buildTaskName
        )
    }

    if (!packaging.generateLoader.get()) return

    val loaderDir = layout.buildDirectory.dir("generated/jni/kotlin")
    val loaderPackage = resolveLoaderPackageName(extension, projectName)

    // Only meaningful with per-platform publishing: a missing library in a bundled build is a
    // defect in this project, not a declaration the consumer forgot, and naming a coordinate
    // would send them down the wrong path.
    val platformsForHint = if (publishPerPlatform) {
        resolveSelectedPlatforms(packaging.publishing, platformId)
    } else {
        emptyList()
    }
    val artifactName = extension.project.name.orNull ?: projectName

    val loader = tasks.register<GenerateNativeLoader>(KreateTasks.Jni.LOADER) {
        packageName.set(loaderPackage)
        this.resourcePath.set(resourcePath)
        outputDirectory.set(loaderDir)
        publishedPlatforms.set(platformsForHint)
        artifactCoordinate.set(
            if (publishPerPlatform) "${project.group}:$artifactName:${project.version}" else ""
        )
    }

    addGeneratedSourceDirectory(loaderDir, loader.name)
}

/**
 * Registers the task that writes the digest manifest, or returns null when it is switched off.
 *
 * The manifest is one file per build and carries the host platform's entry, which is the only
 * platform this build produced a binary for. A build that packages several platforms publishes one
 * artifact each, and each carries its own.
 *
 * @param packaging The packaging configuration.
 * @param libraryDir The directory the native build writes its artifacts to.
 * @param platformId The `<os>-<arch>` identifier the libraries were built for.
 * @param buildTaskName The name of the native build task the manifest must wait for.
 * @return The registered task, or null.
 * @since 3.0.0
 */
private fun Project.registerDigestManifest(
    packaging: JniPackagingExtension,
    libraryDir: Provider<out org.gradle.api.file.Directory>,
    platformId: String,
    buildTaskName: String
): TaskProvider<GenerateDigestManifest>? {
    if (!packaging.digestManifest.get()) return null

    return tasks.register<GenerateDigestManifest>(KreateTasks.Jni.DIGEST_MANIFEST) {
        dependsOn(buildTaskName)
        libraryDirectory.set(libraryDir)
        this.platformId.set(platformId)
        manifest.set(
            layout.buildDirectory.file(
                "jni/$platformId/${GenerateDigestManifest.MANIFEST_FILE_NAME}"
            )
        )
    }
}

/**
 * Resolves the JDK home the native code is compiled against.
 *
 * Taking the path from the Gradle toolchain rather than leaving it to CMake's own search
 * is what guarantees that the JNI headers match the JVM the Kotlin code targets.
 *
 * @param extension The Kreate configuration extension.
 * @return A provider of the absolute JDK home path.
 * @since 2.0.0
 */
private fun Project.resolveToolchainJavaHome(extension: KreateExtension): Provider<String> {
    val toolchains = extensions.getByType(JavaToolchainService::class.java)
    val majorVersion = extension.platform.javaVersion.get().majorVersion.toInt()

    return toolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(majorVersion))
    }.map { it.metadata.installationPath.asFile.absolutePath }
}

/**
 * Returns the directories holding the project's compiled JVM main classes.
 *
 * Handles both plain Kotlin/JVM projects and the JVM target of a multiplatform project.
 * The returned collection carries its producing task dependencies, so a consumer only has
 * to declare it as an input.
 *
 * @return The compiled main class directories, empty when no JVM output exists.
 * @since 2.0.0
 */
private fun Project.jvmMainClassDirectories(): FileCollection {
    val multiplatformOutput = extensions.findByType(KotlinMultiplatformExtension::class.java)
        ?.targets
        ?.withType(KotlinJvmTarget::class.java)
        ?.firstOrNull()
        ?.compilations
        ?.getByName("main")
        ?.output
        ?.allOutputs

    if (multiplatformOutput != null) return files(multiplatformOutput)

    return extensions.findByType(JavaPluginExtension::class.java)
        ?.sourceSets
        ?.get(SourceSet.MAIN_SOURCE_SET_NAME)
        ?.output
        ?.classesDirs
        ?: files()
}

/**
 * Adds a generated directory to the project's main Kotlin sources.
 *
 * @param directory The generated source directory.
 * @param builtBy The name of the task producing it.
 * @return Unit
 * @since 2.0.0
 */
private fun Project.addGeneratedSourceDirectory(
    directory: Provider<out org.gradle.api.file.Directory>,
    builtBy: String
) {
    val multiplatform = extensions.findByType(KotlinMultiplatformExtension::class.java)
    if (multiplatform != null) {
        multiplatform.sourceSets.getByName("jvmMain").kotlin.srcDir(files(directory).builtBy(builtBy))
        return
    }

    val java = extensions.findByType(JavaPluginExtension::class.java) ?: return
    java.sourceSets[SourceSet.MAIN_SOURCE_SET_NAME].java.srcDir(files(directory).builtBy(builtBy))
}

/**
 * Resolves the package the generated loader is placed in.
 *
 * @param extension The Kreate configuration extension.
 * @param projectName The sanitized native project name.
 * @return The fully qualified package name.
 * @since 2.0.0
 */
private fun Project.resolveLoaderPackageName(extension: KreateExtension, projectName: String): String {
    val base = if (extension.project.name.isPresent) extension.project.name.get() else name
    return "$group.$base.jni".lowercase().replace(" ", "").replace("-", "")
        .ifBlank { "$projectName.jni" }
}
