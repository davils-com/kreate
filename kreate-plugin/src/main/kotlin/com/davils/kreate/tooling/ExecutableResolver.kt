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

import com.davils.kreate.host.HostPlatform
import com.davils.kreate.host.OperatingSystem
import java.io.File

private const val PATH_VARIABLE: String = "PATH"

private const val PATHEXT_VARIABLE: String = "PATHEXT"

private const val USER_HOME_PROPERTY: String = "user.home"

private val DEFAULT_WINDOWS_EXTENSIONS: List<String> = listOf(".EXE", ".CMD", ".BAT")

internal class ExecutableResolver(
    private val host: HostPlatform,
    private val userHome: String?
) {
    fun resolve(tool: ExternalTool, override: String? = null): String =
        override?.takeIf { it.isNotBlank() }
            ?: findOnPath(tool)?.absolutePath
            ?: findInWellKnownDirectories(tool)?.absolutePath
            ?: tool.commandName

    private fun findOnPath(tool: ExternalTool): File? {
        val pathValue = System.getenv(PATH_VARIABLE) ?: return null
        val entries = pathValue.split(File.pathSeparatorChar)
        val directories = entries.filter { it.isNotBlank() }.map(::File)
        return findExecutable(directories, candidateNames(tool))
    }

    private fun findInWellKnownDirectories(tool: ExternalTool): File? {
        val directories = tool.wellKnownDirectories.map(::File) + homeRelativeDirectories(tool)
        return findExecutable(directories, candidateNames(tool))
    }

    private fun homeRelativeDirectories(tool: ExternalTool): List<File> {
        val home = userHome ?: return emptyList()
        return tool.homeRelativeDirectories.map { relative -> File(home, relative) }
    }

    private fun findExecutable(directories: List<File>, candidateNames: List<String>): File? {
        val candidates = directories.asSequence().flatMap { directory -> candidatesIn(directory, candidateNames) }
        return candidates.firstOrNull { it.isFile && it.canExecute() }
    }

    private fun candidatesIn(directory: File, candidateNames: List<String>): Sequence<File> =
        candidateNames.asSequence().map { name -> File(directory, name) }

    private fun candidateNames(tool: ExternalTool): List<String> {
        val isWindowsHost = host.operatingSystemOrNull() == OperatingSystem.WINDOWS
        if (!isWindowsHost) return listOf(tool.commandName)

        val windowsNames = windowsExecutableExtensions().map { extension -> tool.commandName + extension.lowercase() }
        return windowsNames + tool.commandName
    }

    private fun windowsExecutableExtensions(): List<String> {
        val declaredExtensions = System.getenv(PATHEXT_VARIABLE) ?: return DEFAULT_WINDOWS_EXTENSIONS
        val entries = declaredExtensions.split(File.pathSeparatorChar)
        return entries.filter { it.isNotBlank() }
    }

    companion object {
        fun resolve(tool: ExternalTool, override: String? = null): String = current().resolve(tool, override)

        private fun current(): ExecutableResolver {
            val userHome = System.getProperty(USER_HOME_PROPERTY)
            return ExecutableResolver(HostPlatform.current(), userHome)
        }
    }
}
