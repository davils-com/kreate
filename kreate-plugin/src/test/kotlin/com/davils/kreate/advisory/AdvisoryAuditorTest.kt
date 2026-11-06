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
package com.davils.kreate.advisory

import com.davils.kreate.advisory.osv.OsvCache
import com.davils.kreate.advisory.osv.OsvClient
import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.dependency.ResolvedGraph
import com.davils.kreate.freshDirectory
import com.davils.kreate.net.HttpTransports
import com.davils.kreate.net.LoopbackServer
import com.davils.kreate.net.NetworkSettings
import com.davils.kreate.net.RetryPolicy
import com.davils.kreate.net.Sleeper
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Clock

private val PUBLIC_MODULE: ModuleCoordinate = ModuleCoordinate("org.example", "lib", "1.0.0")

private val INTERNAL_MODULE: ModuleCoordinate = ModuleCoordinate("com.acme.secret", "billing", "1.0.0")

private const val ANSWER: String = """{"results":[{"vulns":[{"id":"GHSA-1"}]}]}"""

class AdvisoryAuditorTest : FunSpec({
    val workspace = tempdir()
    var server = LoopbackServer.http()

    beforeEach { server = LoopbackServer.http() }
    afterEach { server.close() }

    fun auditor(internalGroups: Set<String> = emptySet()): AdvisoryAuditor {
        val cache = OsvCache(workspace.freshDirectory("cache").toPath())
        val policy = RetryPolicy(maxAttempts = 1)
        val transport = HttpTransports.create(NetworkSettings("test"), Clock.systemUTC(), Sleeper { }, policy)
        val osvUrl = server.uri("").toString()
        return AdvisoryAuditor({ OsvClient(transport, osvUrl, cache) }, internalGroups, Clock.systemUTC())
    }

    fun graphOf(vararg modules: ModuleCoordinate): List<ResolvedGraph> =
        listOf(ResolvedGraph(modules.toList(), emptyList()))

    context("the audit") {
        test("turns a record into a finding with module, fix and link") {
            server.respond("/v1/querybatch", body = ANSWER.toByteArray())
            val record = """
                {"id":"GHSA-1","aliases":["CVE-1"],"summary":"A flaw","database_specific":{"severity":"HIGH"},
                 "affected":[{"package":{"name":"org.example:lib"},"ranges":[
                   {"type":"ECOSYSTEM","events":[{"introduced":"0"},{"fixed":"1.0.1"}]}]}]}
            """.trimIndent()
            server.respond("/v1/vulns/GHSA-1", body = record.toByteArray())

            val document = auditor().audit(graphOf(PUBLIC_MODULE), offline = false)

            document.status shouldBe DocumentStatus.COMPLETE
            val finding = document.findings.single()
            finding.module shouldBe "org.example:lib"
            finding.version shouldBe "1.0.0"
            finding.fixedVersion shouldBe "1.0.1"
            finding.aliases shouldBe listOf("CVE-1")
            finding.url.orEmpty() shouldContain "GHSA-1"
        }

        test("leaves out a withdrawn advisory") {
            server.respond("/v1/querybatch", body = ANSWER.toByteArray())
            server.respond("/v1/vulns/GHSA-1", body = """{"id":"GHSA-1","withdrawn":"2026-01-01"}""".toByteArray())

            val document = auditor().audit(graphOf(PUBLIC_MODULE), offline = false)

            document.findings.shouldBeEmpty()
        }

        test("records when the data was read") {
            server.respond("/v1/querybatch", body = """{"results":[{}]}""".toByteArray())

            val document = auditor().audit(graphOf(PUBLIC_MODULE), offline = false)

            (document.dataAsOf != null) shouldBe true
        }
    }

    context("privacy") {
        test("never sends a module of an internal group, or of a subgroup, to the service") {
            server.respond("/v1/querybatch", body = """{"results":[{}]}""".toByteArray())

            auditor(setOf("com.acme")).audit(graphOf(PUBLIC_MODULE, INTERNAL_MODULE), offline = false)

            val sent = server.requests.joinToString { request -> request.body.decodeToString() }
            sent shouldContain "org.example"
            sent.contains("acme") shouldBe false
            sent.contains("billing") shouldBe false
        }

        test("does not mistake a similar group for an internal one") {
            server.respond("/v1/querybatch", body = """{"results":[{}]}""".toByteArray())
            val similar = ModuleCoordinate("com.acmecorp", "lib", "1.0.0")

            auditor(setOf("com.acme")).audit(graphOf(similar), offline = false)

            server.requests.single().body.decodeToString() shouldContain "com.acmecorp"
        }
    }

    context("what cannot be audited") {
        test("offline is incomplete, and asks nothing") {
            val document = auditor().audit(graphOf(PUBLIC_MODULE), offline = true)

            document.status shouldBe DocumentStatus.INCOMPLETE
            server.requests.shouldBeEmpty()
        }

        test("a service failure is incomplete") {
            server.respond("/v1/querybatch", status = 500)

            val document = auditor().audit(graphOf(PUBLIC_MODULE), offline = false)

            document.status shouldBe DocumentStatus.INCOMPLETE
        }

        test("an unresolved dependency is incomplete, because nobody looked at it") {
            server.respond("/v1/querybatch", body = """{"results":[{}]}""".toByteArray())
            val graphs = listOf(ResolvedGraph(listOf(PUBLIC_MODULE), listOf("org.example:missing:1.0")))

            val document = auditor().audit(graphs, offline = false)

            document.status shouldBe DocumentStatus.INCOMPLETE
            document.reason.orEmpty() shouldContain "org.example:missing:1.0"
        }
    }
})
