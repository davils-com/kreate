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

package com.davils.kreate.host

import com.davils.kore.core.text.SafeText
import org.gradle.api.GradleException
import com.davils.knot.platform.Architecture as HostArchitecture
import com.davils.knot.platform.OperatingSystem as HostSystem

private const val OS_NAME_PROPERTY: String = "os.name"

private const val OS_ARCH_PROPERTY: String = "os.arch"

internal class HostPlatform(
    private val osName: String?,
    private val archName: String?
) {
    fun operatingSystemOrNull(): OperatingSystem? {
        val name = osName ?: return null
        return when (HostSystem.resolve(name)) {
            HostSystem.Linux -> OperatingSystem.LINUX
            HostSystem.MacOs -> OperatingSystem.MACOS
            HostSystem.Windows -> OperatingSystem.WINDOWS
            HostSystem.Wasm, null -> null
        }
    }

    fun requireOperatingSystem(purpose: String): OperatingSystem = operatingSystemOrNull()
        ?: throw GradleException("Cannot determine $purpose: unsupported operating system ${shown(osName)}.")

    fun architectureOrNull(): Architecture? = when (HostArchitecture.resolve(archName.orEmpty())) {
        HostArchitecture.X64 -> Architecture.X64
        HostArchitecture.Arm64 -> Architecture.ARM64
        null -> null
    }

    fun requireArchitecture(purpose: String): Architecture = architectureOrNull()
        ?: throw GradleException("Cannot determine $purpose: unsupported architecture ${shown(archName)}.")

    fun platformId(): String {
        val purpose = "the native platform"
        val operatingSystem = requireOperatingSystem(purpose)
        return platformIdOf(operatingSystem, requireArchitecture(purpose))
    }

    private fun shown(name: String?): String = SafeText.quoted(name.orEmpty())

    companion object {
        fun current(): HostPlatform {
            val osName = System.getProperty(OS_NAME_PROPERTY)
            val archName = System.getProperty(OS_ARCH_PROPERTY)
            return HostPlatform(osName, archName)
        }
    }
}
