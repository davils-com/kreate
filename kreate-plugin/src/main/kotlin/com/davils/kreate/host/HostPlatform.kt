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

import org.gradle.api.GradleException

private const val OS_NAME_PROPERTY: String = "os.name"

private const val OS_ARCH_PROPERTY: String = "os.arch"

internal class HostPlatform(
    private val osName: String?,
    private val archName: String?
) {
    fun operatingSystemOrNull(): OperatingSystem? {
        val normalizedName = osName?.lowercase() ?: return null
        return OperatingSystem.entries.firstOrNull { operatingSystem -> operatingSystem.matches(normalizedName) }
    }

    fun requireOperatingSystem(purpose: String): OperatingSystem = operatingSystemOrNull()
        ?: throw GradleException("Cannot determine $purpose: unsupported operating system '$osName'.")

    fun architectureOrNull(): Architecture? {
        val normalizedName = archName.orEmpty().lowercase()
        return Architecture.entries.firstOrNull { architecture -> architecture.matches(normalizedName) }
    }

    fun requireArchitecture(purpose: String): Architecture = architectureOrNull()
        ?: throw GradleException("Cannot determine $purpose: unsupported architecture '$archName'.")

    fun platformId(): String {
        val purpose = "the native platform"
        val operatingSystem = requireOperatingSystem(purpose)
        return platformIdOf(operatingSystem, requireArchitecture(purpose))
    }

    companion object {
        fun current(): HostPlatform {
            val osName = System.getProperty(OS_NAME_PROPERTY)
            val archName = System.getProperty(OS_ARCH_PROPERTY)
            return HostPlatform(osName, archName)
        }
    }
}
