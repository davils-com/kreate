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

package com.davils.kreate.tooling

import com.davils.kreate.host.OperatingSystem

private const val HOMEBREW_BIN: String = "/opt/homebrew/bin"

private const val LOCAL_BIN: String = "/usr/local/bin"

private const val SYSTEM_BIN: String = "/usr/bin"

private val LINUX_BIN_DIRECTORIES: List<String> = listOf(LOCAL_BIN, SYSTEM_BIN)

private val MACOS_BIN_DIRECTORIES: List<String> = listOf(HOMEBREW_BIN, LOCAL_BIN, SYSTEM_BIN)

private val UNIX_BIN_DIRECTORIES: Map<OperatingSystem, List<String>> = mapOf(
    OperatingSystem.LINUX to LINUX_BIN_DIRECTORIES,
    OperatingSystem.MACOS to MACOS_BIN_DIRECTORIES
)

internal enum class ExternalTool(
    val commandName: String,
    private val directoriesByOperatingSystem: Map<OperatingSystem, List<String>>,
    val homeRelativeDirectories: List<String> = emptyList()
) {
    CMAKE(
        commandName = "cmake",
        directoriesByOperatingSystem = mapOf(
            OperatingSystem.LINUX to LINUX_BIN_DIRECTORIES,
            OperatingSystem.MACOS to MACOS_BIN_DIRECTORIES + "/Applications/CMake.app/Contents/bin",
            OperatingSystem.WINDOWS to listOf("C:\\Program Files\\CMake\\bin")
        )
    ),

    CARGO(
        commandName = "cargo",
        directoriesByOperatingSystem = UNIX_BIN_DIRECTORIES,
        homeRelativeDirectories = listOf(".cargo/bin")
    ),

    TRIVY(
        commandName = "trivy",
        directoriesByOperatingSystem = UNIX_BIN_DIRECTORIES
    ),
    GIT(
        commandName = "git",
        directoriesByOperatingSystem = mapOf(
            OperatingSystem.LINUX to LINUX_BIN_DIRECTORIES,
            OperatingSystem.MACOS to MACOS_BIN_DIRECTORIES,
            OperatingSystem.WINDOWS to listOf("C:\\Program Files\\Git\\cmd")
        )
    );

    fun wellKnownDirectories(operatingSystem: OperatingSystem): List<String> =
        directoriesByOperatingSystem[operatingSystem].orEmpty()
}
