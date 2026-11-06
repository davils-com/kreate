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

import com.sun.net.httpserver.HttpServer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.HexFormat
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory

private const val VERSION: String = "9.9.9"

private const val HTTP_OK: Int = 200

private const val FAKE_TRIVY: String = """#!/bin/sh
while [ ${'$'}# -gt 0 ]; do
    if [ "${'$'}1" = "--output" ]; then
        printf '{}' > "${'$'}2"
    fi
    shift
done
exit 0
"""

private fun realTarOf(directory: File): ByteArray {
    val binary = directory.resolve("trivy")
    binary.writeText(FAKE_TRIVY)
    binary.setExecutable(true)
    val archive = directory.resolve("archive.tar.gz")
    val tar = ProcessBuilder("tar", "-czf", archive.path, "-C", directory.path, "trivy")
    tar.start().waitFor()
    return archive.readBytes()
}

private fun sha256(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return HexFormat.of().formatHex(digest.digest(bytes))
}

private fun hostPlatform(): String {
    val isArm = System.getProperty("os.arch").contains("aarch64")
    val osName = System.getProperty("os.name").lowercase()
    val isMac = osName.contains("mac")
    if (isMac && isArm) return "macOS-ARM64"
    if (isMac) return "macOS-64bit"
    if (isArm) return "Linux-ARM64"
    return "Linux-64bit"
}

class TrivyProvisionFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()
    val archive = realTarOf(createTempDirectory(workspace.toPath(), "archive").toFile())
    val platform = hostPlatform()
    val downloads = AtomicInteger()
    val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
    server.createContext("/releases/v$VERSION/trivy_${VERSION}_$platform.tar.gz") { exchange ->
        downloads.incrementAndGet()
        exchange.sendResponseHeaders(HTTP_OK, archive.size.toLong())
        exchange.responseBody.use { body -> body.write(archive) }
    }
    server.start()
    afterSpec { server.stop(0) }

    val baseUrl = "http://${server.address.address.hostAddress}:${server.address.port}/releases"

    fun newBuild(checksum: String): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.write("gradle.lockfile", "empty=\n")
        fixture.writeBuild(
            """
            ${KreateBuildFixture.platformBlock}

            trivy {
                enabled = true
                version = "$VERSION"
                checksums = mapOf("$platform" to "$checksum")
                secrets { runOnCheck = false }
            }
            """.trimIndent()
        )
        return fixture
    }

    context("Trivy provisioning").config(enabledOrReasonIf = requiresPosixShell) {
        test("downloads the pinned version and runs it") {
            val fixture = newBuild(sha256(archive))

            val result = fixture.build("kreateTrivyVulnerabilityScan", "-Pkreate.trivy.downloadBaseUrl=$baseUrl")

            result.task(":kreateTrivyVulnerabilityScan")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        test("the provision task names the Trivy it found") {
            val fixture = newBuild(sha256(archive))

            val result = fixture.build("kreateTrivyProvision", "-Pkreate.trivy.downloadBaseUrl=$baseUrl")

            result.output shouldContain "Trivy: "
            result.output shouldContain VERSION
        }

        test("refuses a download that does not match its checksum") {
            val fixture = newBuild("0".repeat(64))
            fixture.write(
                "gradle.properties",
                "kreate.trivy.downloadBaseUrl=$baseUrl\n"
            )

            val result = fixture.buildAndFail("kreateTrivyProvision", "-g", workspace.resolve("home-tampered").path)

            result.output shouldContain "Refusing to run it"
        }
    }
})
