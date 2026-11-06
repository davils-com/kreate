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
import com.davils.kreate.host.OperatingSystem
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.api.GradleException
import java.io.File

private const val AMD64: String = "amd64"

private val LINUX_HOST: HostPlatform = HostPlatform("Linux", AMD64)

private val WINDOWS_HOST: HostPlatform = HostPlatform("Windows 11", AMD64)

private val UNKNOWN_HOST: HostPlatform = HostPlatform("Plan 9", AMD64)

private val WINDOWS_ABSOLUTE_PATH: Regex = Regex("""^[A-Z]:\\.+""")

private fun installExecutable(directory: File, name: String): File {
    directory.mkdirs()
    val executable = directory.resolve(name)
    executable.writeText("")
    executable.setExecutable(true)
    return executable
}

private fun environmentOf(vararg variables: Pair<String, String>): (String) -> String? {
    val values = mapOf(*variables)
    return { name -> values[name] }
}

class ExecutableResolverTest : FunSpec({
    val workspace = tempdir()
    val emptyEnvironment = environmentOf()

    context("ExecutableResolver") {
        test("an explicit override always wins") {
            ExecutableResolver.resolve(ExternalTool.CMAKE, "/opt/custom/cmake") shouldBe "/opt/custom/cmake"
        }

        test("a non-existent override is returned unchanged so the failure names the user's own setting") {
            ExecutableResolver.resolve(ExternalTool.CMAKE, "/does/not/exist/cmake") shouldBe
                "/does/not/exist/cmake"
        }

        test("a blank override is ignored") {
            val directory = workspace.freshDirectory("bin")
            val executable = installExecutable(directory, "cargo")
            val resolver = ExecutableResolver(LINUX_HOST, null, environmentOf("PATH" to directory.path))

            resolver.resolve(ExternalTool.CARGO, "   ") shouldBe executable.absolutePath
        }

        test("resolves a tool on the PATH to its absolute path") {
            val directory = workspace.freshDirectory("bin")
            val executable = installExecutable(directory, "cargo")
            val resolver = ExecutableResolver(LINUX_HOST, null, environmentOf("PATH" to directory.path))

            val resolved = File(resolver.resolve(ExternalTool.CARGO))

            resolved shouldBe executable.absoluteFile
            resolved.isAbsolute shouldBe true
        }

        test("skips a PATH entry that holds a file without the execute permission")
            .config(enabledIf = { File.separatorChar == '/' }) {
                val blocked = workspace.freshDirectory("blocked")
                installExecutable(blocked, "cargo").setExecutable(false)
                val allowed = workspace.freshDirectory("allowed")
                val executable = installExecutable(allowed, "cargo")
                val path = blocked.path + File.pathSeparator + allowed.path
                val resolver = ExecutableResolver(LINUX_HOST, null, environmentOf("PATH" to path))

                resolver.resolve(ExternalTool.CARGO) shouldBe executable.absolutePath
            }

        test("ignores a relative PATH entry, which would resolve against the Gradle daemon's directory") {
            val directory = workspace.freshDirectory("planted")
            installExecutable(directory, "cargo")
            val workingDirectory = File("").absoluteFile
            val relativeEntry = directory.relativeTo(workingDirectory).path
            val home = workspace.freshDirectory("home")
            val cargo = installExecutable(home.resolve(".cargo/bin"), "cargo")
            val resolver = ExecutableResolver(WINDOWS_HOST, home.path, environmentOf("PATH" to relativeEntry))

            resolver.resolve(ExternalTool.CARGO) shouldBe cargo.absolutePath
        }

        test("finds a tool installed below the user's home directory") {
            val home = workspace.freshDirectory("home")
            val executable = installExecutable(home.resolve(".cargo/bin"), "cargo")

            val resolved = ExecutableResolver(WINDOWS_HOST, home.path, emptyEnvironment).resolve(ExternalTool.CARGO)

            resolved shouldBe executable.absolutePath
        }

        test("prefers the Windows executable extensions on a Windows host") {
            val home = workspace.freshDirectory("home")
            val directory = home.resolve(".cargo/bin")
            installExecutable(directory, "cargo")
            val windowsExecutable = installExecutable(directory, "cargo.exe")

            val resolved = ExecutableResolver(WINDOWS_HOST, home.path, emptyEnvironment).resolve(ExternalTool.CARGO)

            resolved shouldBe windowsExecutable.absolutePath
        }

        test("honours PATHEXT on a Windows host") {
            val home = workspace.freshDirectory("home")
            val executable = installExecutable(home.resolve(".cargo/bin"), "cargo.ps1")
            val environment = environmentOf("PATHEXT" to ".PS1")

            val resolved = ExecutableResolver(WINDOWS_HOST, home.path, environment).resolve(ExternalTool.CARGO)

            resolved shouldBe executable.absolutePath
        }

        test("fails with the searched directories instead of handing a bare command name to the OS") {
            val home = workspace.freshDirectory("home")
            val resolver = ExecutableResolver(WINDOWS_HOST, home.path, emptyEnvironment)

            val failure = shouldThrow<GradleException> { resolver.resolve(ExternalTool.CARGO) }

            failure.message.orEmpty() shouldContain "'cargo'"
            failure.message.orEmpty() shouldContain home.path
        }

        test("skips the home relative directories when the home directory is unknown") {
            val resolver = ExecutableResolver(WINDOWS_HOST, null, emptyEnvironment)

            shouldThrow<GradleException> { resolver.resolve(ExternalTool.CARGO) }
        }

        test("searches no system directory on an operating system it does not know") {
            val resolver = ExecutableResolver(UNKNOWN_HOST, null, emptyEnvironment)

            val failure = shouldThrow<GradleException> { resolver.resolve(ExternalTool.CMAKE) }

            failure.message.orEmpty() shouldNotContain "/usr/bin"
        }
    }

    context("ExternalTool") {
        test("a Windows host never searches a Unix directory") {
            ExternalTool.entries.forEach { tool ->
                val directories = tool.wellKnownDirectories(OperatingSystem.WINDOWS)

                directories.forEach { directory -> directory.matches(WINDOWS_ABSOLUTE_PATH) shouldBe true }
            }
        }

        test("a Unix host never searches a Windows directory") {
            val unixHosts = listOf(OperatingSystem.LINUX, OperatingSystem.MACOS)

            ExternalTool.entries.forEach { tool ->
                val directories = unixHosts.flatMap { host -> tool.wellKnownDirectories(host) }

                directories.forEach { directory -> directory.startsWith("/") shouldBe true }
            }
        }

        test("CMake is found in its installer's directory on Windows") {
            val directories = ExternalTool.CMAKE.wellKnownDirectories(OperatingSystem.WINDOWS)

            directories shouldContain "C:\\Program Files\\CMake\\bin"
        }

        test("the macOS application bundle is searched on macOS only") {
            val bundle = "/Applications/CMake.app/Contents/bin"

            ExternalTool.CMAKE.wellKnownDirectories(OperatingSystem.MACOS) shouldContain bundle
            ExternalTool.CMAKE.wellKnownDirectories(OperatingSystem.LINUX) shouldNotContain bundle
        }

        test("cargo is searched in the rustup location on every platform") {
            ExternalTool.CARGO.homeRelativeDirectories shouldBe listOf(".cargo/bin")
        }
    }
})
