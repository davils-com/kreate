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

import com.davils.kreate.archive.gzip
import com.davils.kreate.archive.tarOf
import com.davils.kreate.freshDirectory
import com.davils.kreate.host.Architecture
import com.davils.kreate.host.OperatingSystem
import com.davils.kreate.net.HttpTransports
import com.davils.kreate.net.LoopbackServer
import com.davils.kreate.net.NetworkSettings
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.file.shouldNotExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Clock
import java.util.HexFormat

private const val VERSION: String = "9.9.9"

private val ASSET: TrivyAsset = TrivyAsset("Linux-64bit", "tar.gz", "trivy")

private val BINARY: ByteArray = "#!/bin/sh\necho trivy\n".toByteArray()

private fun sha256(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return HexFormat.of().formatHex(digest.digest(bytes))
}

class TrivyProvisionerTest : FunSpec({
    val workspace = tempdir()
    var server = LoopbackServer.http()
    val archive = gzip(
        tarOf(
            "LICENSE" to "text".toByteArray(),
            "contrib/trivy" to "decoy".toByteArray(),
            "trivy" to BINARY
        )
    )
    val archivePath = "/releases/v$VERSION/trivy_${VERSION}_Linux-64bit.tar.gz"
    val checksumPath = "/releases/v$VERSION/trivy_${VERSION}_checksums.txt"

    beforeEach { server = LoopbackServer.http() }
    afterEach { server.close() }

    fun installerFor(warnings: MutableList<String> = mutableListOf()): TrivyInstaller {
        val transport = HttpTransports.create(NetworkSettings("kreate-test"), Clock.systemUTC())
        return TrivyInstaller(transport, server.uri("/releases").toString(), warnings::add)
    }

    fun settings(
        cache: File,
        checksums: Map<String, String> = mapOf("Linux-64bit" to sha256(archive)),
        useInstalled: Boolean = false,
        executable: String? = null,
        offline: Boolean = false
    ): TrivyProvisionSettings = TrivyProvisionSettings(
        version = VERSION,
        useInstalled = useInstalled,
        executable = executable,
        checksums = checksums,
        cacheRoot = cache.toPath(),
        offline = offline,
        asset = ASSET
    )

    fun provisioner(
        settings: TrivyProvisionSettings,
        installed: String? = null,
        installedVersion: String? = null,
        warnings: MutableList<String> = mutableListOf()
    ): TrivyProvisioner = TrivyProvisioner(
        settings = settings,
        installed = { installed },
        versionOf = { installedVersion },
        installer = { installerFor(warnings) },
        warn = warnings::add
    )

    context("resolving Trivy") {
        test("takes the executable a build names, without looking further") {
            val cache = workspace.freshDirectory("cache")

            provisioner(settings(cache, executable = "/opt/trivy")).provision() shouldBe "/opt/trivy"
        }

        test("uses whatever is installed when the build allows it") {
            val cache = workspace.freshDirectory("cache")

            provisioner(settings(cache, useInstalled = true), installed = "/usr/bin/trivy").provision() shouldBe
                "/usr/bin/trivy"
        }

        test("uses an installed Trivy of exactly the pinned version") {
            val cache = workspace.freshDirectory("cache")

            provisioner(settings(cache), "/usr/bin/trivy", VERSION).provision() shouldBe "/usr/bin/trivy"
        }

        test("does not settle for an installed Trivy of another version") {
            server.respond(archivePath, body = archive)
            val cache = workspace.freshDirectory("cache")

            val provisioned = provisioner(settings(cache), "/usr/bin/trivy", "0.1.0").provision()

            provisioned shouldContain cache.path
        }

        test("says what to do when offline, and that the installed one is the wrong version") {
            val cache = workspace.freshDirectory("cache")
            val provisioner = provisioner(settings(cache, offline = true), "/usr/bin/trivy", "0.1.0")

            val failure = shouldThrow<TrivyUnavailableException> { provisioner.provision() }

            failure.message.orEmpty() shouldContain "offline"
            failure.message.orEmpty() shouldContain "another version"
        }

        test("reports a platform Trivy publishes nothing for") {
            val cache = workspace.freshDirectory("cache")
            val settings = settings(cache).copy(asset = null)

            shouldThrow<TrivyUnavailableException> { provisioner(settings).provision() }
        }
    }

    context("installing") {
        test("verifies, extracts the top-level binary and leaves it executable") {
            server.respond(archivePath, body = archive)
            val cache = workspace.freshDirectory("cache")

            val binary = File(provisioner(settings(cache)).provision())

            binary.readBytes() shouldBe BINARY
            binary.canExecute() shouldBe true
        }

        test("leaves no temporary directory behind") {
            server.respond(archivePath, body = archive)
            val cache = workspace.freshDirectory("cache")

            val binary = File(provisioner(settings(cache)).provision())

            val installed = binary.parentFile.listFiles().orEmpty()
            installed.map { file -> file.name }.sorted() shouldBe listOf("trivy", "trivy.sha256")
        }

        test("refuses an archive that does not match its pinned checksum, and installs nothing") {
            server.respond(archivePath, body = gzip(tarOf("trivy" to "tampered".toByteArray())))
            val cache = workspace.freshDirectory("cache")

            val failure = shouldThrow<TrivyUnavailableException> { provisioner(settings(cache)).provision() }

            failure.message.orEmpty() shouldContain "Refusing to run it"
            cache.resolve("$VERSION/Linux-64bit/trivy").shouldNotExist()
        }

        test("falls back to the published checksums and warns that they prove less") {
            server.respond(archivePath, body = archive)
            val listing = "${sha256(archive)}  trivy_${VERSION}_Linux-64bit.tar.gz\n"
            server.respond(checksumPath, body = listing.toByteArray())
            val cache = workspace.freshDirectory("cache")
            val warnings = mutableListOf<String>()

            provisioner(settings(cache, checksums = emptyMap()), warnings = warnings).provision()

            warnings.single() shouldContain "not that the release is genuine"
        }

        test("refuses a version whose checksums do not list the platform") {
            server.respond(archivePath, body = archive)
            server.respond(checksumPath, body = "abc  something-else\n".toByteArray())
            val cache = workspace.freshDirectory("cache")

            shouldThrow<TrivyUnavailableException> {
                provisioner(settings(cache, checksums = emptyMap())).provision()
            }
        }

        test("reports a failed download as unavailable") {
            val cache = workspace.freshDirectory("cache")
            val settings = settings(cache).copy(version = "0.0.0")

            shouldThrow<TrivyUnavailableException> { provisioner(settings).provision() }
        }

        test("refuses an archive without the binary") {
            val empty = gzip(tarOf("LICENSE" to "text".toByteArray()))
            server.respond(archivePath, body = empty)
            val cache = workspace.freshDirectory("cache")
            val settings = settings(cache, checksums = mapOf("Linux-64bit" to sha256(empty)))

            shouldThrow<TrivyUnavailableException> { provisioner(settings).provision() }
        }
    }

    context("the cache") {
        test("serves a second run without a download") {
            server.respond(archivePath, body = archive)
            val cache = workspace.freshDirectory("cache")
            val first = provisioner(settings(cache)).provision()
            val before = server.requests.size

            provisioner(settings(cache)).provision() shouldBe first

            server.requests.size shouldBe before
        }

        test("notices a cached binary that changed, and installs it again") {
            server.respond(archivePath, body = archive)
            val cache = workspace.freshDirectory("cache")
            val binary = File(provisioner(settings(cache)).provision())
            binary.writeText("tampered")
            val warnings = mutableListOf<String>()

            provisioner(settings(cache), warnings = warnings).provision()

            binary.readBytes() shouldBe BINARY
            warnings.single() shouldContain "was changed"
        }

        test("treats a binary without its digest as not installed") {
            server.respond(archivePath, body = archive)
            val cache = workspace.freshDirectory("cache")
            val binary = File(provisioner(settings(cache)).provision())
            Files.delete(TrivyInstaller.digestFileOf(binary.toPath()))
            binary.delete()

            provisioner(settings(cache)).provision()

            binary.shouldExist()
        }
    }

    context("the assets of a host") {
        test("cover the platforms Trivy publishes") {
            TrivyAssets.forHost(OperatingSystem.LINUX, Architecture.X64)?.platform shouldBe "Linux-64bit"
            TrivyAssets.forHost(OperatingSystem.LINUX, Architecture.ARM64)?.platform shouldBe "Linux-ARM64"
            TrivyAssets.forHost(OperatingSystem.MACOS, Architecture.ARM64)?.platform shouldBe "macOS-ARM64"
            TrivyAssets.forHost(OperatingSystem.WINDOWS, Architecture.X64)?.binaryName shouldBe "trivy.exe"
        }

        test("know that Windows on ARM has no build") {
            TrivyAssets.forHost(OperatingSystem.WINDOWS, Architecture.ARM64) shouldBe null
        }

        test("name the release archive") {
            ASSET.archiveName("0.76.2") shouldBe "trivy_0.76.2_Linux-64bit.tar.gz"
        }

        test("carry a pinned checksum for every platform of the default version") {
            listOf("Linux-64bit", "Linux-ARM64", "macOS-64bit", "macOS-ARM64", "windows-64bit").forEach { platform ->
                TrivyPins.digestOf(TrivyPins.DEFAULT_VERSION, platform)?.length shouldBe 64
            }
            TrivyPins.digestOf("0.0.1", "Linux-64bit") shouldBe null
        }
    }
})
