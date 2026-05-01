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

package com.davils.kreate.module.platform.multiplatform.cinterop

import com.davils.kreate.KreateExtension
import com.davils.kreate.module.platform.resolveFeatureProjectName
import com.davils.kreate.system.Architecture
import com.davils.kreate.system.OsTarget
import com.davils.kreate.system.getArchitecture
import com.davils.kreate.system.getOs
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import java.io.File

/**
 * Resolves the name for the C-interop project.
 *
 * It uses the override from [CInteropExtension] if present, otherwise it
 * defaults to the project name from the extension or the Gradle project name.
 *
 * @param extension The Kreate configuration extension.
 * @return The resolved and sanitized project name.
 * @since 1.0.0
 */
internal fun Project.resolveProjectName(extension: KreateExtension): String {
    val cInteropConfig = extension.platform.multiplatform.cInterop
    return resolveFeatureProjectName(extension, cInteropConfig.nameOverride)
}

/**
 * Resolves the root directory for C-interop files.
 *
 * @param cInteropConfig The C-interop configuration extension.
 * @return The resolved root directory.
 * @since 1.0.0
 */
internal fun Project.resolveRootDir(cInteropConfig: CInteropExtension): File {
    if (cInteropConfig.projectDirectory.isPresent) {
        return projectDir.resolve(cInteropConfig.projectDirectory.get().asFile)
    }
    return projectDir.resolve("cinterop")
}

/**
 * Resolves the list of Rust targets for compilation.
 *
 * If targets are explicitly provided in the configuration, they are used.
 * Otherwise, the targets are inferred from the current OS and architecture.
 *
 * @param rustTargets The property containing the list of Rust targets.
 * @return The resolved list of Rust target strings.
 * @throws GradleException If the current OS is unsupported.
 * @since 1.0.0
 */
internal fun resolveRustTargets(rustTargets: ListProperty<String>): List<String> {
    if (rustTargets.isPresent && rustTargets.get().isNotEmpty()) {
        return rustTargets.get()
    }

    val arch by getArchitecture()
    val os by getOs()

    return when (os) {
        OsTarget.WINDOWS -> listOf("x86_64-pc-windows-gnu")
        OsTarget.LINUX -> if (arch == Architecture.X64) {
            listOf("x86_64-unknown-linux-gnu")
        } else {
            listOf("aarch64-unknown-linux-gnu")
        }
        OsTarget.MACOS -> listOf("aarch64-apple-darwin")
        else -> throw GradleException("Unsupported OS: $os")
    }
}

/**
 * Resolves the path to the Cargo executable.
 *
 * @return The resolved Cargo command string.
 * @since 1.0.0
 */
internal fun resolveCargoCommand(): String {
    val os by getOs()
    return if (os == OsTarget.MACOS) {
        "${System.getProperty("user.home")}/.cargo/bin/cargo"
    } else {
        "cargo"
    }
}
