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
package com.davils.kreate.trivy.provision

import com.davils.kreate.host.Architecture
import com.davils.kreate.host.OperatingSystem

private const val TAR_EXTENSION: String = "tar.gz"

private const val UNIX_BINARY: String = "trivy"

private const val WINDOWS_BINARY: String = "trivy.exe"

internal object TrivyAssets {
    fun forHost(operatingSystem: OperatingSystem, architecture: Architecture): TrivyAsset? =
        when (operatingSystem) {
            OperatingSystem.LINUX -> TrivyAsset(linuxPlatform(architecture), TAR_EXTENSION, UNIX_BINARY)
            OperatingSystem.MACOS -> TrivyAsset(macosPlatform(architecture), TAR_EXTENSION, UNIX_BINARY)
            OperatingSystem.WINDOWS -> windowsAsset(architecture)
        }

    private fun linuxPlatform(architecture: Architecture): String = when (architecture) {
        Architecture.X64 -> "Linux-64bit"
        Architecture.ARM64 -> "Linux-ARM64"
    }

    private fun macosPlatform(architecture: Architecture): String = when (architecture) {
        Architecture.X64 -> "macOS-64bit"
        Architecture.ARM64 -> "macOS-ARM64"
    }

    private fun windowsAsset(architecture: Architecture): TrivyAsset? {
        if (architecture != Architecture.X64) return null
        return TrivyAsset("windows-64bit", TrivyAsset.ZIP_EXTENSION, WINDOWS_BINARY)
    }
}
