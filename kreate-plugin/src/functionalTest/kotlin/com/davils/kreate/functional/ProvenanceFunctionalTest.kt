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

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Duration
import java.time.Instant

private const val HTTP_OK: Int = 200

private const val HTTP_NOT_FOUND: Int = 404

private const val DEPENDENCY: String = "org.apache.commons:commons-lang3:3.14.0"

private const val PACKAGE_PATH: String = "/v3/systems/maven/packages/org.apache.commons%3Acommons-lang3"

private const val METADATA_PATH: String = "/maven2/org/example/kreate/internal-lib/maven-metadata.xml"

private val RUN_PREFIX: Regex = Regex("^/run\\d+")

private const val TRUST_DOCUMENT: String = "build/kreate/assurance/documents/trust.json"

private const val CONFUSION_DOCUMENT: String = "build/kreate/assurance/documents/confusion.json"

private fun HttpExchange.answer(status: Int, text: String = "") {
    val body = text.toByteArray()
    sendResponseHeaders(status, body.size.toLong().takeIf { size -> size > 0 } ?: -1)
    responseBody.use { stream -> stream.write(body) }
}

private fun pomOf(group: String, artifact: String, dependencies: String = ""): String = """
    <project>
      <modelVersion>4.0.0</modelVersion>
      <groupId>$group</groupId>
      <artifactId>$artifact</artifactId>
      <version>1.0</version>
      <packaging>pom</packaging>
      $dependencies
    </project>
""".trimIndent()

private fun KreateBuildFixture.publish(group: String, artifact: String, dependencies: String = "") {
    val path = "local-repository/${group.replace('.', '/')}/$artifact/1.0/$artifact-1.0.pom"
    write(path, pomOf(group, artifact, dependencies))
}

private fun KreateBuildFixture.prepend(text: String) {
    val script = file("build.gradle.kts")
    write("build.gradle.kts", text + "\n\n" + script.readText())
}

private const val LOCAL_REPOSITORIES: String = """
    repositories {
        maven { url = uri("local-repository") }
        mavenCentral()
    }
"""

class ProvenanceFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()
    var published = Instant.parse("2020-01-01T00:00:00Z")
    val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)

    fun answerTo(exchange: HttpExchange) {
        val path = exchange.requestURI.rawPath.replace(RUN_PREFIX, "")
        if (path == METADATA_PATH) {
            exchange.answer(HTTP_OK, "<metadata><versioning><release>99.0</release></versioning></metadata>")
            return
        }
        if (path != PACKAGE_PATH) return exchange.answer(HTTP_NOT_FOUND)
        val first = """{"versionKey":{"version":"1.0"},"publishedAt":"2015-01-01T00:00:00Z"}"""
        val current = """{"versionKey":{"version":"3.14.0"},"publishedAt":"$published"}"""
        exchange.answer(HTTP_OK, """{"versions":[$first,$current]}""")
    }
    server.createContext("/") { exchange -> answerTo(exchange) }
    server.start()
    afterSpec { server.stop(0) }

    val address = "http://${server.address.address.hostAddress}:${server.address.port}"

    var run = 0

    fun depsDevUrl(): String {
        run += 1
        return "-Pkreate.network.depsDevUrl=$address/run$run"
    }

    beforeEach { published = Instant.parse("2020-01-01T00:00:00Z") }

    fun newBuild(assuranceBlock: String, dependency: String = DEPENDENCY): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.writeBuild(
            kreateBlock = """
                ${KreateBuildFixture.platformBlock}

                assurance {
                    enabled = true
                    $assuranceBlock
                }
            """.trimIndent(),
            extra = """
                $LOCAL_REPOSITORIES

                dependencies { implementation("$dependency") }
            """.trimIndent()
        )
        return fixture
    }

    context("the trust assessment") {
        test("is not there unless it is enabled") {
            val fixture = newBuild("confusion { enabled = true }")

            val tasks = fixture.build("tasks", "--all").output

            tasks.contains("kreateDependencyTrust") shouldBe false
            tasks shouldContain "kreateDependencyConfusion"
        }

        test("passes a dependency with a long history") {
            val fixture = newBuild("trust { enabled = true }")

            val result = fixture.build("kreateAssurance", depsDevUrl())

            result.output shouldContain "Kreate assurance [PASSED]"
            val document = fixture.file(TRUST_DOCUMENT).readText()
            document shouldContain "TRUST_DATA_UNAVAILABLE"
            document.contains("commons-lang3:3.14.0: deps.dev does not know") shouldBe false
            result.output.contains("TRUST_DATA_UNAVAILABLE org.apache.commons:commons-lang3") shouldBe false
        }

        test("blocks a version published an hour ago") {
            published = Instant.now().minus(Duration.ofHours(1))
            val fixture = newBuild("trust { enabled = true }")

            val result = fixture.buildAndFail("kreateAssurance", depsDevUrl())

            result.output shouldContain "Kreate assurance [FINDINGS]"
            result.output shouldContain "FRESH_RELEASE"
        }

        test("is incomplete offline") {
            val fixture = newBuild("trust { enabled = true }")
            fixture.build("dependencies", "--configuration", "runtimeClasspath")

            val result = fixture.buildAndFail("kreateAssurance", "--offline", depsDevUrl())

            result.output shouldContain "Kreate assurance [INCOMPLETE]"
        }
    }

    context("the confusion check") {
        test("blocks a typosquatted group") {
            val fixture = newBuild("confusion { enabled = true }", dependency = "com.goog1e.guava:guava:1.0")
            fixture.publish("com.goog1e.guava", "guava")

            val result = fixture.buildAndFail("kreateAssurance")

            result.output shouldContain "Kreate assurance [FINDINGS]"
            fixture.file(CONFUSION_DOCUMENT).readText() shouldContain "TYPOSQUAT_GROUP"
        }

        test("an allowed group passes") {
            val block = """confusion { enabled = true; allow = setOf("com.goog1e.guava") }"""
            val fixture = newBuild(block, dependency = "com.goog1e.guava:guava:1.0")
            fixture.publish("com.goog1e.guava", "guava")

            fixture.build("kreateAssurance").output shouldContain "Kreate assurance [PASSED]"
        }

        test("reports a plugin marker that leads into a foreign namespace") {
            val fixture = newBuild("confusion { enabled = true }")
            val evil = "<dependencies><dependency><groupId>io.evil</groupId><artifactId>payload</artifactId>" +
                "<version>1.0</version><type>pom</type></dependency></dependencies>"
            fixture.publish("com.acme.build", "com.acme.build.gradle.plugin", evil)
            fixture.publish("io.evil", "payload")
            fixture.prepend(
                """
                buildscript {
                    repositories { maven { url = uri("local-repository") } }
                    dependencies { classpath("com.acme.build:com.acme.build.gradle.plugin:1.0") }
                }
                """.trimIndent()
            )

            fixture.build("kreateDependencyConfusion")

            val document = fixture.file(CONFUSION_DOCUMENT).readText()
            document shouldContain "PLUGIN_MARKER_MISMATCH"
            document shouldContain "io.evil:payload"
        }

        test("finds an internal module in the public repository only when asked to") {
            val block = """
                internalGroups = setOf("org.example.kreate")
                confusion {
                    enabled = true
                    checkPublicRepository = true
                    publicRepository = "$address/maven2"
                }
            """.trimIndent()
            val fixture = newBuild(block, dependency = "org.example.kreate:internal-lib:1.0")
            fixture.publish("org.example.kreate", "internal-lib")

            val result = fixture.buildAndFail("kreateAssurance")

            result.output shouldContain "DEPENDENCY_CONFUSION"
            fixture.file(CONFUSION_DOCUMENT).readText() shouldContain "\"publicVersion\": \"99.0\""
        }

        test("reuses the configuration cache") {
            val fixture = newBuild("confusion { enabled = true }")
            fixture.build("kreateDependencyConfusion")

            val second = fixture.build("kreateDependencyConfusion")

            second.output shouldContain "Reusing configuration cache"
        }
    }
})
