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
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

private const val HTTP_OK: Int = 200

private const val PAST_THE_WINDOW_MILLIS: Long = 1100

private const val DEPENDENCY: String = "org.apache.commons:commons-lang3:3.14.0"

private fun resultFor(query: String, vulnerable: Boolean): String {
    if (vulnerable && "commons-lang3" in query) return """{"vulns":[{"id":"GHSA-1"}]}"""
    return "{}"
}

class AdvisoryFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()
    val queries = CopyOnWriteArrayList<String>()
    var vulnerable = false
    val record = """
        {"id":"GHSA-1","summary":"A flaw","database_specific":{"severity":"HIGH"},
         "affected":[{"package":{"name":"org.apache.commons:commons-lang3"},"ranges":[
           {"type":"ECOSYSTEM","events":[{"introduced":"0"},{"fixed":"3.18.0"}]}]}]}
    """.trimIndent()
    val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
    server.createContext("/v1/querybatch") { exchange ->
        val query = exchange.requestBody.readBytes().decodeToString()
        queries += query
        val entries = query.split("\"ecosystem\"").drop(1)
        val results = entries.map { entry -> resultFor(entry, vulnerable) }
        val body = """{"results":[${results.joinToString(",")}]}""".toByteArray()
        exchange.sendResponseHeaders(HTTP_OK, body.size.toLong())
        exchange.responseBody.use { stream -> stream.write(body) }
    }
    server.createContext("/v1/vulns/GHSA-1") { exchange ->
        val body = record.toByteArray()
        exchange.sendResponseHeaders(HTTP_OK, body.size.toLong())
        exchange.responseBody.use { stream -> stream.write(body) }
    }
    server.start()
    afterSpec { server.stop(0) }

    val osvUrl = "-Pkreate.network.osvUrl=http://${server.address.address.hostAddress}:${server.address.port}"

    beforeEach {
        queries.clear()
        vulnerable = false
    }

    fun newBuild(assuranceBlock: String = ""): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.writeBuild(
            kreateBlock = """
                ${KreateBuildFixture.platformBlock}

                assurance {
                    enabled = true
                    advisories { enabled = true }
                    $assuranceBlock
                }
            """.trimIndent(),
            extra = """dependencies { implementation("$DEPENDENCY") }"""
        )
        return fixture
    }

    context("the advisory audit") {
        test("is not there unless it is enabled") {
            val fixture = KreateBuildFixture.createIn(workspace)
            fixture.writeSettings()
            fixture.writeBuild("""${KreateBuildFixture.platformBlock}

                assurance { enabled = true }
            """.trimIndent())

            fixture.build("tasks", "--all").output.contains("kreateAdvisoryAudit") shouldBe false
        }

        test("asks OSV about the resolved dependencies") {
            val fixture = newBuild()

            fixture.build("kreateAdvisoryAudit", osvUrl)

            queries.single() shouldContain "org.apache.commons:commons-lang3"
            queries.single() shouldContain "3.14.0"
        }

        test("feeds the gate") {
            val fixture = newBuild()
            vulnerable = true

            val result = fixture.buildAndFail("kreateAssurance", osvUrl)

            result.output shouldContain "HIGH GHSA-1 org.apache.commons:commons-lang3:3.14.0, fixed in 3.18.0: A flaw"
            result.output shouldContain "Kreate assurance [FINDINGS]"
        }

        test("an ignore rule accepts an advisory") {
            val fixture = newBuild("""ignore("GHSA-1") { reason = "Not reachable." }""")
            vulnerable = true

            val result = fixture.build("kreateAssurance", osvUrl)

            result.output shouldContain "Kreate assurance [PASSED]"
        }

        test("keeps an internal group away from the service") {
            val fixture = newBuild("""internalGroups = setOf("org.apache.commons")""")

            fixture.build("kreateAdvisoryAudit", osvUrl)

            queries.joinToString().contains("commons-lang3") shouldBe false
        }

        test("is incomplete when a dependency does not resolve") {
            val fixture = KreateBuildFixture.createIn(workspace)
            fixture.writeSettings()
            fixture.writeBuild(
                kreateBlock = """
                    ${KreateBuildFixture.platformBlock}

                    assurance {
                        enabled = true
                        advisories { enabled = true }
                    }
                """.trimIndent(),
                extra = """dependencies { implementation("org.example.kreate:missing:1.0") }"""
            )

            val result = fixture.buildAndFail("kreateAssurance", osvUrl)

            result.output shouldContain "Kreate assurance [INCOMPLETE]"
        }

        test("is incomplete in offline mode") {
            val fixture = newBuild()
            fixture.build("dependencies", "--configuration", "runtimeClasspath")

            val result = fixture.buildAndFail("kreateAssurance", "--offline", osvUrl)

            result.output shouldContain "Kreate assurance [INCOMPLETE]"
        }

        test("is up to date inside the reuse window, and the configuration cache still applies") {
            val fixture = newBuild()
            fixture.build("kreateAdvisoryAudit", osvUrl)

            val second = fixture.build("kreateAdvisoryAudit", osvUrl)

            second.task(":kreateAdvisoryAudit")?.outcome shouldBe TaskOutcome.UP_TO_DATE
            second.output shouldContain "Reusing configuration cache"
            queries.size shouldBe 1
        }

        test("runs again once the reuse window is over, without invalidating the configuration cache") {
            val fixture = newBuild("reuseVerdictFor = Duration.ofSeconds(1)")
            val script = fixture.file("build.gradle.kts")
            fixture.write("build.gradle.kts", "import java.time.Duration\n\n" + script.readText())
            fixture.build("kreateAdvisoryAudit", osvUrl)
            Thread.sleep(PAST_THE_WINDOW_MILLIS)

            val second = fixture.build("kreateAdvisoryAudit", osvUrl)

            second.task(":kreateAdvisoryAudit")?.outcome shouldBe TaskOutcome.SUCCESS
            second.output shouldContain "Reusing configuration cache"
            queries.size shouldBe 2
        }
    }
})
