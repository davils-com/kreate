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

package com.davils.kreate.functional

import io.kotest.core.test.Enabled
import io.kotest.core.test.EnabledOrReasonIf
import java.io.File

private const val WINDOWS_MARKER: String = "win"

private val WELL_KNOWN_DIRECTORIES: List<String> = listOf("/opt/homebrew/bin", "/usr/local/bin", "/usr/bin")

val requiresCmake: EnabledOrReasonIf = { requireTool("cmake", "KREATE_REQUIRE_CMAKE") }

val requiresTrivy: EnabledOrReasonIf = { requireTool("trivy", "KREATE_REQUIRE_TRIVY") }

val requiresPosixShell: EnabledOrReasonIf = { posixShell() }

fun isWindows(): Boolean {
    val operatingSystem = System.getProperty("os.name")
    return operatingSystem.lowercase().contains(WINDOWS_MARKER)
}

private fun posixShell(): Enabled {
    if (isWindows()) return Enabled.disabled("The fixture's wrapper is a POSIX shell script.")
    return Enabled.enabled
}

private fun requireTool(tool: String, requiredVariable: String): Enabled {
    if (isOnPath(tool)) return Enabled.enabled

    check(System.getenv(requiredVariable) == null) {
        "$tool is required but was not found on PATH, and $requiredVariable forbids skipping its tests."
    }
    return Enabled.disabled("$tool is not installed")
}

private fun isOnPath(tool: String): Boolean {
    val names = executableNames(tool)
    val path = System.getenv("PATH").orEmpty()
    val pathDirectories = path.split(File.pathSeparatorChar)
    val directories = pathDirectories + WELL_KNOWN_DIRECTORIES
    val searched = directories.filter { directory -> directory.isNotBlank() }
    return searched.any { directory -> names.any { name -> File(directory, name).canExecute() } }
}

private fun executableNames(tool: String): List<String> {
    if (isWindows()) return listOf("$tool.exe", tool)
    return listOf(tool)
}
