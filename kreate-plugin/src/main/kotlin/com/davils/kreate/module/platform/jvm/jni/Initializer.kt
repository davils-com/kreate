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
import com.davils.kreate.jobs.executeTaskBeforeCompile
import com.davils.kreate.module.platform.jvm.jni.tasks.BuildNative
import com.davils.kreate.module.platform.jvm.jni.tasks.InitializeCppProject
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import java.io.File

/**
 * Initializes the JNI configuration for the project.
 *
 * Mirrors the C-interop feature layout: sources live under
 * `<projectDirectory>/<projectName>/src` with a `CMakeLists.txt` at
 * `<projectDirectory>/<projectName>/CMakeLists.txt`.
 *
 * When enabled this registers the full task chain and hooks the native build
 * into the Kotlin compile pipeline, and wires `-Djava.library.path` into any
 * [Test] and [JavaExec] task so the runtime can resolve the shared library.
 *
 * @param extension The Kreate configuration extension.
 * @return Unit
 * @since 1.1.0
 */
public fun Project.initializeJni(extension: KreateExtension) {
    val jniConfig = extension.platform.jvm.jni
    if (!jniConfig.enabled.get()) return

    addJniTasks(extension)
    applyRuntimeLibraryPath(extension)
}

private fun validateRootDir(rootDir: File) {
    if (!rootDir.exists() && !rootDir.mkdirs()) {
        throw GradleException("Failed to create root directory: ${rootDir.absolutePath}")
    }
}

private fun Project.addJniTasks(extension: KreateExtension) {
    val jniConfig = extension.platform.jvm.jni
    val projectName = resolveProjectName(extension)

    val rootDir = resolveRootDir(jniConfig)
    validateRootDir(rootDir)
    val nativeProjectDir = rootDir.resolve(projectName)

    val initializeJniProject by tasks.register<InitializeCppProject>("kreate-jni-initialize") {
        this.workDir.set(rootDir)
        this.projectName.set(projectName)
    }

    val buildNative by tasks.register<BuildNative>(KREATE_JNI_BUILD_TASK) {
        this.workDir.set(nativeProjectDir)
        dependsOn(initializeJniProject)
    }

    executeTaskBeforeCompile(buildNative)
}

private fun Project.applyRuntimeLibraryPath(extension: KreateExtension) {
    val jniConfig = extension.platform.jvm.jni
    val projectName = resolveProjectName(extension)
    val nativeLibDir = resolveRootDir(jniConfig).resolve(projectName).resolve("build")

    tasks.withType<Test>().configureEach {
        dependsOn(KREATE_JNI_BUILD_TASK)
        jvmArgs("$JAVA_LIBRARY_PATH_PROPERTY${nativeLibDir.absolutePath}")
    }
    tasks.withType<JavaExec>().configureEach {
        dependsOn(KREATE_JNI_BUILD_TASK)
        jvmArgs("$JAVA_LIBRARY_PATH_PROPERTY${nativeLibDir.absolutePath}")
    }
}

private const val KREATE_JNI_BUILD_TASK = "kreate-jni-build"
private const val JAVA_LIBRARY_PATH_PROPERTY = "-Djava.library.path="
