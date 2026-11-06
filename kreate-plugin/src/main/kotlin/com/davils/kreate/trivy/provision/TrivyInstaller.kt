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

import com.davils.kreate.net.HttpMethod
import com.davils.kreate.net.HttpTransport
import com.davils.kreate.net.TransportRequest
import java.net.URI
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

private const val MAX_ARCHIVE_BYTES: Int = 256 * 1024 * 1024

private const val MAX_CHECKSUM_FILE_BYTES: Int = 1024 * 1024

private const val DIGEST_SUFFIX: String = ".sha256"

private val GITHUB_RELEASE_HOSTS: Set<String> = setOf(
    "github.com",
    "release-assets.githubusercontent.com",
    "objects.githubusercontent.com"
)

internal class TrivyInstaller(
    private val transport: HttpTransport,
    private val downloadBaseUrl: String,
    private val warn: (String) -> Unit
) {
    private val baseUri: URI = URI(downloadBaseUrl.trimEnd('/'))

    private val allowedHosts: Set<String> = allowedHostsOf(baseUri)

    @OptIn(ExperimentalPathApi::class)
    fun install(version: String, asset: TrivyAsset, expectedDigest: String?, directory: Path): Path {
        Files.createDirectories(directory)
        val workspace = Files.createTempDirectory(directory, ".install")

        return try {
            val archive = workspace.resolve(asset.archiveName(version))
            download(releaseUri(version, asset.archiveName(version)), archive)
            verify(archive, expectedDigest ?: publishedDigest(version, asset))

            val binary = workspace.resolve(asset.binaryName)
            TrivyArchive.extract(archive, asset, binary)
            binary.toFile().setExecutable(true, false)
            publish(binary, directory.resolve(asset.binaryName))
        } finally {
            workspace.deleteRecursively()
        }
    }

    private fun download(uri: URI, target: Path) {
        val request = TransportRequest(
            method = HttpMethod.GET,
            uri = uri,
            maxBytes = MAX_ARCHIVE_BYTES,
            allowedHosts = allowedHosts,
            downloadTo = target
        )
        val response = transport.send(request)
        if (!response.isSuccess) {
            throw TrivyUnavailableException("Downloading $uri failed with status ${response.statusCode}.")
        }
    }

    private fun verify(archive: Path, expected: String) {
        val actual = sha256Of(archive)
        if (actual.equals(expected, ignoreCase = true)) return

        throw TrivyUnavailableException(
            "${archive.fileName} has the SHA-256 $actual, not $expected. Refusing to run it."
        )
    }

    private fun publishedDigest(version: String, asset: TrivyAsset): String {
        warn(
            "Trivy $version has no pinned checksum for ${asset.platform}. Its own checksum file proves that " +
                "the download is intact, not that the release is genuine: set trivy { checksums } to pin it."
        )

        val name = "trivy_${version}_checksums.txt"
        val request = TransportRequest(
            method = HttpMethod.GET,
            uri = releaseUri(version, name),
            maxBytes = MAX_CHECKSUM_FILE_BYTES,
            allowedHosts = allowedHosts
        )
        val response = transport.send(request)
        if (!response.isSuccess) {
            throw TrivyUnavailableException("Trivy $version publishes no checksums at ${request.uri}.")
        }
        return digestIn(response.text, asset.archiveName(version))
            ?: throw TrivyUnavailableException("The checksums of Trivy $version do not list ${asset.platform}.")
    }

    private fun digestIn(checksums: String, archiveName: String): String? {
        val lines = checksums.lineSequence().map { line -> line.trim().split(Regex("\\s+")) }
        val matching = lines.firstOrNull { parts -> parts.lastOrNull()?.removePrefix("*") == archiveName }
        return matching?.firstOrNull()
    }

    private fun releaseUri(version: String, fileName: String): URI =
        URI("${baseUri.toString().trimEnd('/')}/v$version/$fileName")

    private fun publish(binary: Path, target: Path): Path {
        val digest = sha256Of(binary)
        val digestFile = digestFileOf(target)
        try {
            Files.move(binary, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (cause: FileAlreadyExistsException) {
            warn("Another build installed Trivy at the same time: ${cause.message}")
            return target
        } catch (cause: AtomicMoveNotSupportedException) {
            throw TrivyUnavailableException("The Trivy cache does not support an atomic move: ${cause.message}", cause)
        }
        Files.writeString(digestFile, digest)
        return target
    }

    companion object {
        fun digestFileOf(binary: Path): Path = binary.resolveSibling(binary.fileName.toString() + DIGEST_SUFFIX)

        private fun allowedHostsOf(uri: URI): Set<String> {
            val host = uri.host.orEmpty()
            if (host == "github.com") return GITHUB_RELEASE_HOSTS
            return setOf(host)
        }
    }
}
