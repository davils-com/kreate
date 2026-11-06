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
package com.davils.kreate.advisory.osv

import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.freshDirectory
import com.davils.kreate.net.HttpTransports
import com.davils.kreate.net.LoopbackServer
import com.davils.kreate.net.NetworkSettings
import com.davils.kreate.net.RetryPolicy
import com.davils.kreate.net.Sleeper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Clock

private val MODULE: ModuleCoordinate = ModuleCoordinate("org.example", "lib", "1.0.0")

private const val RECORD: String =
    """{"id":"GHSA-1","modified":"2026-01-01T00:00:00Z","summary":"A flaw"}"""

private const val ANSWER: String =
    """{"results":[{"vulns":[{"id":"GHSA-1","modified":"2026-01-01T00:00:00Z"}]}]}"""

class OsvClientTest : FunSpec({
    val workspace = tempdir()
    var server = LoopbackServer.http()

    beforeEach { server = LoopbackServer.http() }
    afterEach { server.close() }

    fun clientFor(cache: OsvCache = OsvCache(workspace.freshDirectory("cache").toPath())): OsvClient {
        val settings = NetworkSettings("kreate-test")
        val transport = HttpTransports.create(settings, Clock.systemUTC(), Sleeper { }, RetryPolicy(maxAttempts = 1))
        return OsvClient(transport, server.uri("").toString(), cache)
    }

    context("OSV") {
        test("is asked for the Maven ecosystem with group and artifact") {
            server.respond("/v1/querybatch", body = """{"results":[{}]}""".toByteArray())

            clientFor().recordsOf(listOf(MODULE))

            val body = server.requests.single().body.decodeToString()
            body shouldContain "\"ecosystem\":\"Maven\""
            body shouldContain "\"name\":\"org.example:lib\""
            body shouldContain "\"version\":\"1.0.0\""
        }

        test("lists no record for a clean module") {
            server.respond("/v1/querybatch", body = """{"results":[{}]}""".toByteArray())

            val records = clientFor().recordsOf(listOf(MODULE))

            records.getValue(MODULE).shouldBeEmpty()
        }

        test("fetches the full record of what it lists") {
            server.respond("/v1/querybatch", body = ANSWER.toByteArray())
            server.respond("/v1/vulns/GHSA-1", body = RECORD.toByteArray())

            val records = clientFor().recordsOf(listOf(MODULE))

            records.getValue(MODULE).single().summary shouldBe "A flaw"
        }

        test("follows the page token until the list ends") {
            var call = 0
            val pages = listOf(
                """{"results":[{"vulns":[{"id":"GHSA-1"}],"next_page_token":"next"}]}""",
                """{"results":[{"vulns":[{"id":"GHSA-2"}]}]}"""
            )
            server.handle("/v1/querybatch") { exchange ->
                val page = pages[call].toByteArray()
                call += 1
                exchange.sendResponseHeaders(200, page.size.toLong())
                exchange.responseBody.write(page)
            }
            server.respond("/v1/vulns/GHSA-1", body = RECORD.toByteArray())
            server.respond("/v1/vulns/GHSA-2", body = """{"id":"GHSA-2"}""".toByteArray())

            val records = clientFor().recordsOf(listOf(MODULE))

            val listed = records.getValue(MODULE)
            listed.map { record -> record.id } shouldBe listOf("GHSA-1", "GHSA-2")
            val queries = server.requests.filter { request -> request.path == "/v1/querybatch" }
            val bodies = queries.map { request -> request.body.decodeToString() }
            bodies.first().contains("page_token") shouldBe false
            bodies.last() shouldContain "\"page_token\":\"next\""
        }

        test("splits a large query into batches") {
            val modules = (1..501).map { index -> ModuleCoordinate("org.example", "lib$index", "1.0.0") }
            server.handle("/v1/querybatch") { exchange ->
                val query = server.requests.last().body.decodeToString()
                val parts = query.split("\"ecosystem\"")
                val results = List(parts.size - 1) { "{}" }.joinToString(",")
                val answer = """{"results":[$results]}""".toByteArray()
                exchange.sendResponseHeaders(200, answer.size.toLong())
                exchange.responseBody.write(answer)
            }

            clientFor().recordsOf(modules).size shouldBe 501
        }

        test("asks nothing for no modules") {
            clientFor().recordsOf(emptyList()).size shouldBe 0

            server.requests.shouldBeEmpty()
        }
    }

    context("the record cache") {
        test("serves a record whose modification time still matches") {
            server.respond("/v1/querybatch", body = ANSWER.toByteArray())
            server.respond("/v1/vulns/GHSA-1", body = RECORD.toByteArray())
            val cache = OsvCache(workspace.freshDirectory("cache").toPath())
            clientFor(cache).recordsOf(listOf(MODULE))

            clientFor(cache).recordsOf(listOf(MODULE))

            server.requests.count { it.path == "/v1/vulns/GHSA-1" } shouldBe 1
        }

        test("reads a record again once OSV changed it") {
            server.respond("/v1/querybatch", body = ANSWER.toByteArray())
            server.respond("/v1/vulns/GHSA-1", body = RECORD.toByteArray())
            val cache = OsvCache(workspace.freshDirectory("cache").toPath())
            cache.write("GHSA-1", """{"id":"GHSA-1","modified":"2020-01-01T00:00:00Z","summary":"Stale"}""")

            val records = clientFor(cache).recordsOf(listOf(MODULE))

            records.getValue(MODULE).single().summary shouldBe "A flaw"
        }

        test("keeps an id from leaving the directory") {
            val directory = workspace.freshDirectory("cache")
            val cache = OsvCache(directory.resolve("records").toPath())

            cache.write("../../evil", "{}")

            directory.parentFile.resolve("evil.json").exists() shouldBe false
            cache.read("../../evil") shouldBe "{}"
        }
    }

    context("a failure") {
        test("of the service is reported, not treated as clean") {
            server.respond("/v1/querybatch", status = 500)

            shouldThrow<OsvUnavailableException> { clientFor().recordsOf(listOf(MODULE)) }
        }

        test("of a record is reported") {
            server.respond("/v1/querybatch", body = ANSWER.toByteArray())
            server.respond("/v1/vulns/GHSA-1", status = 404)

            shouldThrow<OsvUnavailableException> { clientFor().recordsOf(listOf(MODULE)) }
        }

        test("to answer every query is reported") {
            server.respond("/v1/querybatch", body = """{"results":[]}""".toByteArray())

            shouldThrow<OsvUnavailableException> { clientFor().recordsOf(listOf(MODULE)) }
        }

        test("to answer with JSON is reported") {
            server.respond("/v1/querybatch", body = "nope".toByteArray())

            shouldThrow<OsvUnavailableException> { clientFor().recordsOf(listOf(MODULE)) }
        }
    }
})
