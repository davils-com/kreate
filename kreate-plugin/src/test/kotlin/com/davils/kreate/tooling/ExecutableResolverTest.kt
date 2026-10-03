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

import com.davils.kreate.freshDirectory
import com.davils.kreate.host.HostPlatform
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File

private const val AMD64: String = "amd64"

private val LINUX_HOST: HostPlatform = HostPlatform("Linux", AMD64)

private val WINDOWS_HOST: HostPlatform = HostPlatform("Windows 11", AMD64)

private fun installExecutable(home: File, name: String): File {
    val directory = home.resolve(".cargo/bin")
    directory.mkdirs()
    val executable = directory.resolve(name)
    executable.writeText("")
    executable.setExecutable(true)
    return executable
}

class ExecutableResolverTest : FunSpec({
    val pathVariable = System.getenv("PATH").orEmpty()
    val pathDirectories: List<String> = pathVariable.split(File.pathSeparatorChar).filter { it.isNotBlank() }

    fun isInstalledOn(command: String, directories: List<String>): Boolean {
        val names = listOf(command, "$command.exe", "$command.cmd", "$command.bat")
        return directories.any { directory -> names.any { File(directory, it).canExecute() } }
    }

    val isCmakeInstalled = isInstalledOn("cmake", pathDirectories)
    val trivySearchDirectories = pathDirectories + ExternalTool.TRIVY.wellKnownDirectories
    val isTrivyInstalled = isInstalledOn("trivy", trivySearchDirectories)
    val cargoSearchDirectories = pathDirectories + ExternalTool.CARGO.wellKnownDirectories
    val isCargoOnSearchPath = isInstalledOn("cargo", cargoSearchDirectories)
    val workspace = tempdir()

    context("ExecutableResolver") {
        test("an explicit override always wins") {
            ExecutableResolver.resolve(ExternalTool.CMAKE, "/opt/custom/cmake") shouldBe "/opt/custom/cmake"
        }

        test("a non-existent override is returned unchanged so the failure names the user's own setting") {
            ExecutableResolver.resolve(ExternalTool.CMAKE, "/does/not/exist/cmake") shouldBe
                "/does/not/exist/cmake"
        }

        test("a blank override is ignored") {
            ExecutableResolver.resolve(ExternalTool.CMAKE, "   ") shouldNotBe "   "
        }

        test("resolves an absolute path for a tool that is installed").config(enabledIf = { isCmakeInstalled }) {
            val resolved = File(ExecutableResolver.resolve(ExternalTool.CMAKE))

            resolved.isAbsolute shouldBe true
            resolved.canExecute() shouldBe true
        }

        test("falls back to the bare command name for a tool that is not installed")
            .config(enabledIf = { !isTrivyInstalled }) {
                ExecutableResolver.resolve(ExternalTool.TRIVY) shouldBe "trivy"
            }

        test("finds a tool installed below the user's home directory")
            .config(enabledIf = { !isCargoOnSearchPath }) {
                val home = workspace.freshDirectory("home")
                val executable = installExecutable(home, "cargo")

                val resolved = ExecutableResolver(LINUX_HOST, home.path).resolve(ExternalTool.CARGO)

                resolved shouldBe executable.absolutePath
            }

        test("prefers the Windows executable extensions on a Windows host")
            .config(enabledIf = { !isCargoOnSearchPath }) {
                val home = workspace.freshDirectory("home")
                installExecutable(home, "cargo")
                val windowsExecutable = installExecutable(home, "cargo.exe")

                val resolved = ExecutableResolver(WINDOWS_HOST, home.path).resolve(ExternalTool.CARGO)

                resolved shouldBe windowsExecutable.absolutePath
            }

        test("falls back to the bare command name when the tool is nowhere to be found")
            .config(enabledIf = { !isCargoOnSearchPath }) {
                val home = workspace.freshDirectory("home")

                val resolved = ExecutableResolver(LINUX_HOST, home.path).resolve(ExternalTool.CARGO)

                resolved shouldBe "cargo"
            }

        test("skips the home relative directories when the home directory is unknown")
            .config(enabledIf = { !isCargoOnSearchPath }) {
                ExecutableResolver(LINUX_HOST, null).resolve(ExternalTool.CARGO) shouldBe "cargo"
            }

        test("every tool declares at least one well known directory") {
            ExternalTool.entries.forEach { tool ->
                val hasSearchPaths =
                    tool.wellKnownDirectories.isNotEmpty() || tool.homeRelativeDirectories.isNotEmpty()

                hasSearchPaths shouldBe true
            }
        }

        test("cargo is searched in the rustup location on every platform") {
            ExternalTool.CARGO.homeRelativeDirectories shouldBe listOf(".cargo/bin")
        }
    }
})
