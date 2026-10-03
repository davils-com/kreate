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

package com.davils.kreate.jni.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.gradle.SourceSetName
import com.davils.kreate.gradle.declaredProjectName
import com.davils.kreate.gradle.resolveFeatureProjectName
import com.davils.kreate.gradle.resolveFeatureRootDirectory
import com.davils.kreate.host.HostPlatform
import com.davils.kreate.jni.JniExtension
import com.davils.kreate.jni.JniPackagingExtension
import com.davils.kreate.jni.JniPublishingExtension
import com.davils.kreate.jni.JniTaskNames
import com.davils.kreate.jni.task.BuildNative
import com.davils.kreate.jni.task.ConfigureNative
import com.davils.kreate.jni.task.DIGEST_MANIFEST_FILE_NAME
import com.davils.kreate.jni.task.GenerateDigestManifest
import com.davils.kreate.jni.task.GenerateJniHeaders
import com.davils.kreate.jni.task.GenerateNativeLoader
import com.davils.kreate.jni.task.InitializeCppProject
import com.davils.kreate.task.CMAKE_LISTS_FILE_NAME
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec
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

private const val JNI_DIRECTORY_NAME: String = "jni"

private const val CMAKE_CACHE_FILE_NAME: String = "CMakeCache.txt"

private const val JVM_MAIN_SOURCE_SET_NAME: String = "jvmMain"

internal fun Project.initializeJni(extension: KreateExtension) {
    val jniConfig = extension.platform.jvm.jni
    if (!jniConfig.enabled.get()) return

    val projectName = resolveFeatureProjectName(extension, jniConfig.nameOverride)
    val rootDirectory = resolveFeatureRootDirectory(jniConfig.projectDirectory, JNI_DIRECTORY_NAME)
    val nativeProjectDir = rootDirectory.resolve(projectName)
    val platformId = HostPlatform.current().platformId()

    val cmakeBuildDir = layout.buildDirectory.dir("jni/$platformId/cmake")
    val libraryDir = layout.buildDirectory.dir("jni/$platformId/lib")
    val headerDir = layout.buildDirectory.dir("generated/jni/include")
    val javaHome = resolveToolchainJavaHome(extension)

    val initialize = tasks.register<InitializeCppProject>(JniTaskNames.INITIALIZE) {
        this.projectName.set(projectName)
        this.projectRoot.set(layout.dir(provider { nativeProjectDir }))
    }

    val headers = tasks.register<GenerateJniHeaders>(JniTaskNames.HEADERS) {
        classDirectories.from(jvmMainClassDirectories())
        headerFileName.set(jniConfig.headers.fileName.orElse("${projectName}_jni.h"))
        outputDirectory.set(headerDir)
        onlyIf { jniConfig.headers.enabled.get() }
    }

    val configure = tasks.register<ConfigureNative>(JniTaskNames.CONFIGURE) {
        sourceDirectory.set(layout.dir(provider { nativeProjectDir }))
        cmakeListsFile.set(layout.file(provider { nativeProjectDir.resolve(CMAKE_LISTS_FILE_NAME) }))
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
        cmakeCache.set(cmakeBuildDir.map { it.file(CMAKE_CACHE_FILE_NAME) })
        dependsOn(initialize, headers)
    }

    val build = tasks.register<BuildNative>(JniTaskNames.BUILD) {
        nativeSources.from(
            fileTree(nativeProjectDir) {
                include(CMAKE_LISTS_FILE_NAME, "src/**", "include/**")
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

private fun Project.applyRuntimeLibraryPath(
    jniConfig: JniExtension,
    libraryDir: Provider<out Directory>,
    buildTaskName: String
) {
    val libraryPath = libraryDir.map { directory -> runtimeLibraryPath(directory, jniConfig) }

    tasks.withType<Test>().configureEach {
        dependsOn(buildTaskName)
        jvmArgumentProviders.add(JavaLibraryPathArgumentProvider(libraryPath))
    }

    tasks.withType<JavaExec>().configureEach {
        dependsOn(buildTaskName)
        jvmArgumentProviders.add(JavaLibraryPathArgumentProvider(libraryPath))
    }
}

private fun Project.runtimeLibraryPath(libraryDirectory: Directory, jniConfig: JniExtension): String {
    val additionalPaths = jniConfig.libraryRuntimePaths.getOrElse(emptyList())
    val paths = listOf(libraryDirectory.asFile.absolutePath) + additionalPaths.map { path -> file(path).absolutePath }
    return paths.joinToString(File.pathSeparator)
}

private fun Project.applyNativePackaging(
    extension: KreateExtension,
    projectName: String,
    libraryDir: Provider<out Directory>,
    platformId: String,
    buildTaskName: String
) {
    val packaging = extension.platform.jvm.jni.packaging
    if (!packaging.enabled.get()) return

    distributeNativeLibraries(extension, projectName, libraryDir, platformId, buildTaskName)

    if (!packaging.generateLoader.get()) return

    registerNativeLoader(extension, projectName, platformId)
}

private fun Project.distributeNativeLibraries(
    extension: KreateExtension,
    projectName: String,
    libraryDir: Provider<out Directory>,
    platformId: String,
    buildTaskName: String
) {
    val packaging = extension.platform.jvm.jni.packaging
    val resourcePath = packaging.resourcePath.get()
    val publishPerPlatform = packaging.publishing.enabled.get()
    val manifest = registerDigestManifest(packaging, libraryDir, platformId, buildTaskName)

    if (publishPerPlatform) {
        configureNativePublishing(
            extension = extension,
            projectName = projectName,
            hostLibraryDir = libraryDir,
            hostPlatformId = platformId,
            resourcePath = resourcePath,
            buildTaskName = buildTaskName
        )
        return
    }

    bundleIntoMainJar(libraryDir, manifest, resourcePath, platformId, buildTaskName)
}

private fun Project.bundleIntoMainJar(
    libraryDir: Provider<out Directory>,
    manifest: TaskProvider<GenerateDigestManifest>?,
    resourcePath: String,
    platformId: String,
    buildTaskName: String
) {
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
}

private fun Project.registerNativeLoader(
    extension: KreateExtension,
    projectName: String,
    platformId: String
) {
    val packaging = extension.platform.jvm.jni.packaging
    val resourcePath = packaging.resourcePath.get()
    val publishing = packaging.publishing
    val publishPerPlatform = publishing.enabled.get()
    val loaderDir = layout.buildDirectory.dir("generated/jni/kotlin")
    val loaderPackage = resolveLoaderPackageName(extension)
    val platformsNamedInHint = platformsNamedInLoaderHint(publishing, publishPerPlatform, platformId)
    val artifactName = extension.project.name.orNull ?: projectName

    val loader = tasks.register<GenerateNativeLoader>(JniTaskNames.LOADER) {
        packageName.set(loaderPackage)
        this.resourcePath.set(resourcePath)
        outputDirectory.set(loaderDir)
        publishedPlatforms.set(platformsNamedInHint)
        artifactCoordinate.set(loaderHintCoordinate(publishPerPlatform, artifactName))
    }

    addGeneratedSourceDirectory(loaderDir, loader.name)
}

private fun Project.platformsNamedInLoaderHint(
    publishing: JniPublishingExtension,
    publishPerPlatform: Boolean,
    platformId: String
): List<String> {
    if (!publishPerPlatform) return emptyList()
    return resolveSelectedPlatforms(publishing, platformId)
}

private fun Project.loaderHintCoordinate(publishPerPlatform: Boolean, artifactName: String): String {
    if (!publishPerPlatform) return ""
    return "$group:$artifactName:$version"
}

private fun Project.registerDigestManifest(
    packaging: JniPackagingExtension,
    libraryDir: Provider<out Directory>,
    platformId: String,
    buildTaskName: String
): TaskProvider<GenerateDigestManifest>? {
    if (!packaging.digestManifest.get()) return null

    return tasks.register<GenerateDigestManifest>(JniTaskNames.DIGEST_MANIFEST) {
        dependsOn(buildTaskName)
        libraryDirectory.set(libraryDir)
        this.platformId.set(platformId)
        manifest.set(layout.buildDirectory.file("jni/$platformId/$DIGEST_MANIFEST_FILE_NAME"))
    }
}

private fun Project.resolveToolchainJavaHome(extension: KreateExtension): Provider<String> {
    val toolchains = extensions.getByType(JavaToolchainService::class.java)
    val majorVersion = extension.platform.javaVersion.get().majorVersion.toInt()
    val launcher = toolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(majorVersion))
    }

    return launcher.map { it.metadata.installationPath.asFile.absolutePath }
}

private fun Project.jvmMainClassDirectories(): FileCollection {
    val multiplatformOutput = multiplatformJvmMainOutput()
    if (multiplatformOutput != null) return files(multiplatformOutput)

    val java = extensions.findByType(JavaPluginExtension::class.java) ?: return files()
    return java.sourceSets[SourceSetName.MAIN].output.classesDirs
}

private fun Project.multiplatformJvmMainOutput(): FileCollection? {
    val multiplatform = extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return null
    val jvmTarget = multiplatform.targets.withType(KotlinJvmTarget::class.java).firstOrNull() ?: return null
    val mainCompilation = jvmTarget.compilations.getByName(SourceSetName.MAIN)
    return mainCompilation.output.allOutputs
}

private fun Project.addGeneratedSourceDirectory(
    directory: Provider<out Directory>,
    builtBy: String
) {
    val generatedSources = files(directory).builtBy(builtBy)
    val multiplatform = extensions.findByType(KotlinMultiplatformExtension::class.java)
    if (multiplatform != null) {
        multiplatform.sourceSets.getByName(JVM_MAIN_SOURCE_SET_NAME).kotlin.srcDir(generatedSources)
        return
    }

    val java = extensions.findByType(JavaPluginExtension::class.java) ?: return
    java.sourceSets[SourceSetName.MAIN].java.srcDir(generatedSources)
}

private fun Project.resolveLoaderPackageName(extension: KreateExtension): String {
    val packageName = "$group.${declaredProjectName(extension)}.jni".lowercase()
    return packageName.replace(" ", "").replace("-", "")
}
