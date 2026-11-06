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

import com.davils.kreate.net.NetworkException
import java.nio.file.Files
import java.nio.file.Path

internal class TrivyProvisioner(
    private val settings: TrivyProvisionSettings,
    private val installed: () -> String?,
    private val versionOf: (String) -> String?,
    private val installer: () -> TrivyInstaller,
    private val warn: (String) -> Unit
) {
    fun provision(): String {
        val executable = settings.executable?.takeIf { path -> path.isNotBlank() }
        if (executable != null) return executable

        val anyInstalled = installed()
        if (settings.useInstalled && anyInstalled != null) return anyInstalled

        val version = settings.version
        val cached = cachedBinary()
        if (cached != null) return cached.toString()
        if (anyInstalled != null && versionOf(anyInstalled) == version) return anyInstalled

        return download(anyInstalled).toString()
    }

    private fun cachedBinary(): Path? {
        val binary = binaryPath() ?: return null
        val digestFile = TrivyInstaller.digestFileOf(binary)
        if (!Files.isRegularFile(binary) || !Files.isRegularFile(digestFile)) return null
        if (sha256Of(binary) == Files.readString(digestFile).trim()) return binary

        warn("The cached Trivy at $binary was changed after it was installed. Installing it again.")
        Files.delete(binary)
        Files.delete(digestFile)
        return null
    }

    private fun binaryPath(): Path? {
        val asset = settings.asset ?: return null
        return directoryOf(asset).resolve(asset.binaryName)
    }

    private fun directoryOf(asset: TrivyAsset): Path {
        val versionDirectory = settings.cacheRoot.resolve(settings.version)
        return versionDirectory.resolve(asset.platform)
    }

    private fun download(installedElsewhere: String?): Path {
        val version = settings.version
        val asset = requireDownloadable(installedElsewhere)
        val expected = settings.checksums[asset.platform] ?: TrivyPins.digestOf(version, asset.platform)
        return try {
            installer().install(version, asset, expected, directoryOf(asset))
        } catch (cause: NetworkException) {
            throw TrivyUnavailableException("Trivy $version could not be downloaded: ${cause.message}", cause)
        }
    }

    private fun requireDownloadable(installedElsewhere: String?): TrivyAsset {
        val asset = settings.asset
            ?: throw TrivyUnavailableException("Trivy publishes no build for this platform. Set trivy { executable }.")
        if (settings.offline) throw TrivyUnavailableException(offlineMessage(settings.version, installedElsewhere))
        return asset
    }

    private fun offlineMessage(version: String, installedElsewhere: String?): String {
        val found = installedElsewhere?.let { path -> " The Trivy at $path is another version." }.orEmpty()
        return "Trivy $version is not installed and this build is offline.$found " +
            "Set trivy { executable } or trivy { useInstalled = true }."
    }
}
