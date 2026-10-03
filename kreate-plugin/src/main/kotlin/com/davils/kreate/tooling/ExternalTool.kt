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

private const val HOMEBREW_BIN = "/opt/homebrew/bin"
private const val LOCAL_BIN = "/usr/local/bin"
private const val SYSTEM_BIN = "/usr/bin"

private val UNIX_BIN_DIRECTORIES = listOf(HOMEBREW_BIN, LOCAL_BIN, SYSTEM_BIN)

internal enum class ExternalTool(
    val commandName: String,
    val wellKnownDirectories: List<String>,
    val homeRelativeDirectories: List<String> = emptyList()
) {
    CMAKE(
        commandName = "cmake",
        wellKnownDirectories = UNIX_BIN_DIRECTORIES +
            listOf(
                "/Applications/CMake.app/Contents/bin",
                "C:\\Program Files\\CMake\\bin"
            )
    ),

    CARGO(
        commandName = "cargo",
        wellKnownDirectories = UNIX_BIN_DIRECTORIES,
        homeRelativeDirectories = listOf(".cargo/bin")
    ),

    TRIVY(
        commandName = "trivy",
        wellKnownDirectories = UNIX_BIN_DIRECTORIES
    )
}
