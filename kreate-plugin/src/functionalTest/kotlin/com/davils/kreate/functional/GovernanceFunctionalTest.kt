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
import io.kotest.matchers.file.shouldNotExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.net.InetAddress
import java.net.InetSocketAddress

private const val HTTP_OK: Int = 200

private const val HTTP_NOT_FOUND: Int = 404

private const val DEPENDENCY: String = "org.apache.commons:commons-lang3:3.14.0"

private const val PACKAGE_PATH: String = "/v3/systems/maven/packages/org.apache.commons%3Acommons-lang3"

private const val POLICY_DOCUMENT: String = "build/kreate/assurance/documents/dependencies.json"

private const val CATALOG: String = "gradle/libs.versions.toml"

private val RUN_PREFIX: Regex = Regex("^/run\\d+")

private val HISTORY: String = """
    {"versions":[
      {"versionKey":{"version":"3.14.0"},"publishedAt":"2023-11-18T00:00:00Z","isDefault":false},
      {"versionKey":{"version":"3.18.0"},"publishedAt":"2025-07-10T00:00:00Z","isDefault":false},
      {"versionKey":{"version":"5.0.0"},"publishedAt":"2026-05-01T00:00:00Z","isDefault":true}
    ]}
""".trimIndent()

private val RECORD: String = """
    {"id":"GHSA-1","modified":"2026-01-01T00:00:00Z","summary":"A flaw","database_specific":{"severity":"HIGH"},
     "affected":[{"package":{"name":"org.apache.commons:commons-lang3"},"ranges":[
       {"type":"ECOSYSTEM","events":[{"introduced":"0"},{"fixed":"3.18.0"}]}]}]}
""".trimIndent()

private fun HttpExchange.answer(status: Int, text: String = "") {
    val body = text.toByteArray()
    sendResponseHeaders(status, body.size.toLong().takeIf { size -> size > 0 } ?: -1)
    responseBody.use { stream -> stream.write(body) }
}

private fun osvResults(query: String, vulnerable: Boolean): String {
    val entries = query.split("\"ecosystem\"").drop(1)
    val results = entries.map { entry -> resultOf(entry, vulnerable) }
    return """{"results":[${results.joinToString(",")}]}"""
}

private fun resultOf(entry: String, vulnerable: Boolean): String {
    val affected = vulnerable && "commons-lang3" in entry && "3.14.0" in entry
    if (affected) return """{"vulns":[{"id":"GHSA-1"}]}"""
    return "{}"
}

class GovernanceFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()
    var vulnerable = false
    val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)

    fun answerTo(exchange: HttpExchange) {
        val path = exchange.requestURI.rawPath.replace(RUN_PREFIX, "")
        if (path == "/v1/querybatch") {
            val query = exchange.requestBody.readBytes().decodeToString()
            return exchange.answer(HTTP_OK, osvResults(query, vulnerable))
        }
        if (path == "/v1/vulns/GHSA-1") return exchange.answer(HTTP_OK, RECORD)
        if (path == PACKAGE_PATH) return exchange.answer(HTTP_OK, HISTORY)
        exchange.answer(HTTP_NOT_FOUND)
    }
    server.createContext("/") { exchange -> answerTo(exchange) }
    server.start()
    afterSpec { server.stop(0) }

    val address = "http://${server.address.address.hostAddress}:${server.address.port}"
    var run = 0

    fun network(): Array<String> {
        run += 1
        return arrayOf("-Pkreate.network.depsDevUrl=$address/run$run", "-Pkreate.network.osvUrl=$address/run$run")
    }

    beforeEach { vulnerable = false }

    fun newBuild(policyBlock: String = "", extra: String = "", dependency: String = DEPENDENCY): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.writeBuild(
            kreateBlock = """
                ${KreateBuildFixture.platformBlock}

                assurance {
                    enabled = true
                    dependencyPolicy {
                        enabled = true
                        $policyBlock
                    }
                    $extra
                }
            """.trimIndent(),
            extra = """dependencies { implementation("$dependency") }"""
        )
        return fixture
    }

    context("the dependency policy") {
        test("blocks a denied module") {
            val fixture = newBuild("""deny("org.apache.commons:commons-lang3", "Use the JDK.")""")

            val result = fixture.buildAndFail("kreateAssurance")

            result.output shouldContain "Kreate assurance [FINDINGS]"
            fixture.file(POLICY_DOCUMENT).readText() shouldContain "DENIED_MODULE"
        }

        test("reports a dynamic version and a build without verification, without blocking") {
            val fixture = newBuild(dependency = "org.apache.commons:commons-lang3:3.+")

            val result = fixture.build("kreateAssurance")

            result.output shouldContain "Kreate assurance [PASSED]"
            val document = fixture.file(POLICY_DOCUMENT).readText()
            document shouldContain "DYNAMIC_VERSION"
            document shouldContain "VERIFICATION_DISABLED"
        }

        test("leaves the plugin repositories of the settings out of the project's repositories") {
            val fixture = newBuild("""allowedRepositoryHosts = setOf("repo.maven.apache.org", "plugins.gradle.org")""")
            fixture.write(
                "settings.gradle.kts",
                """
                pluginManagement {
                    repositories {
                        mavenCentral()
                        gradlePluginPortal()
                    }
                }

                dependencyResolutionManagement {
                    repositories {
                        mavenCentral()
                    }
                }

                rootProject.name = "sample"
                """.trimIndent()
            )

            fixture.build("kreateDependencyPolicy")

            val document = fixture.file(POLICY_DOCUMENT).readText()
            document shouldNotContain "DISALLOWED_REPOSITORY"
            document shouldNotContain "__plugin_repository__"
        }

        test("reuses the configuration cache") {
            val fixture = newBuild()
            fixture.build("kreateDependencyPolicy")

            fixture.build("kreateDependencyPolicy").output shouldContain "Reusing configuration cache"
        }
    }

    context("the outdated check") {
        test("lists what lags behind and reports more majors than allowed, counting only majors that exist") {
            val fixture = newBuild("checkOutdated = true\nmaxMajorBehind = 0")

            val result = fixture.build("kreateDependencyOutdated", *network())

            result.output shouldContain "org.apache.commons:commons-lang3 3.14.0 -> 5.0.0 (1 major"
            result.output shouldContain "OUTDATED_MAJOR"
        }
    }

    context("adding a module") {
        test("writes exactly the checked latest version to the catalog") {
            val fixture = newBuild()

            val result = fixture.build("kreateDependencyAdd", "--module=org.apache.commons:commons-lang3", *network())

            fixture.file(CATALOG).readText() shouldContain
                "commons-lang3 = { module = \"org.apache.commons:commons-lang3\", version.ref = \"commons-lang3\" }"
            fixture.file(CATALOG).readText() shouldContain "commons-lang3 = \"5.0.0\""
            result.output shouldContain "implementation(libs.commons.lang3)"
        }

        test("refuses a vulnerable version, unless forced") {
            vulnerable = true
            val fixture = newBuild()

            val refused = fixture.buildAndFail("kreateDependencyAdd", "--module=$DEPENDENCY", *network())

            refused.output shouldContain "GHSA-1"
            fixture.file(CATALOG).shouldNotExist()
            fixture.build("kreateDependencyAdd", "--module=$DEPENDENCY", "--force", *network())
            fixture.file(CATALOG).readText() shouldContain "commons-lang3 = \"3.14.0\""
        }

        test("does not add what could not be checked, even when forced") {
            val fixture = newBuild()

            val arguments = arrayOf("--module=$DEPENDENCY", "--force", "--offline")

            val result = fixture.buildAndFail("kreateDependencyAdd", *arguments, *network())

            result.output shouldContain "could not be checked"
            fixture.file(CATALOG).shouldNotExist()
        }

        test("changes nothing in a dry run") {
            val fixture = newBuild()

            fixture.build("kreateDependencyAdd", "--module=$DEPENDENCY", "--dry-run", *network())

            fixture.file(CATALOG).shouldNotExist()
        }
    }

    context("fixing the catalog") {
        test("shows the fix of a vulnerable version and applies it on request") {
            vulnerable = true
            val fixture = newBuild(extra = "advisories { enabled = true }")
            val catalog = """
                [versions]
                lang = "3.14.0" # pinned for the test

                [libraries]
                lang = { module = "org.apache.commons:commons-lang3", version.ref = "lang" }
            """.trimIndent()
            fixture.write(CATALOG, catalog)
            val urls = network()

            val shown = fixture.build("kreateDependencyFix", *urls)

            shown.output shouldContain "lang: 3.14.0 -> 3.18.0"
            fixture.file("build/kreate/dependencies/catalog-fix.diff").readText() shouldContain "+lang = \"3.18.0\""
            fixture.file(CATALOG).readText() shouldNotContain "3.18.0"

            fixture.build("kreateDependencyFix", "--apply", *urls)

            fixture.file(CATALOG).readText() shouldBe catalog.replace("3.14.0", "3.18.0") + "\n"
        }
    }
})
