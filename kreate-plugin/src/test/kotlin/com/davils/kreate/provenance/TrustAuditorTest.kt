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
package com.davils.kreate.provenance

import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.dependency.ResolvedGraph
import com.davils.kreate.freshDirectory
import com.davils.kreate.net.FIXED_CLOCK
import com.davils.kreate.net.ScriptedTransport
import com.davils.kreate.net.responseOf
import com.davils.kreate.provenance.depsdev.DepsDevCache
import com.davils.kreate.provenance.depsdev.DepsDevClient
import com.davils.kreate.provenance.trust.TrustThresholds
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Duration

private val MODULE: ModuleCoordinate = ModuleCoordinate("org.example", "lib", "1.0.0")

private val INTERNAL: ModuleCoordinate = ModuleCoordinate("com.acme.secret", "billing", "1.0.0")

private val ROUTES: Map<String, String> = mapOf(
    PACKAGE_PATH to historyJson("0.1.0" to "2020-01-01T00:00:00Z", "1.0.0" to "2026-11-06T10:00:00Z"),
    VERSION_PATH to versionJson("github.com/example/lib"),
    PROJECT_PATH to projectJson(2.0)
)

private fun graphOf(vararg modules: ModuleCoordinate, unresolved: List<String> = emptyList()) =
    listOf(ResolvedGraph(modules.toList(), unresolved))

class TrustAuditorTest : FunSpec({
    val workspace = tempdir()

    fun auditor(transport: ScriptedTransport, minScorecard: Double = 4.0): TrustAuditor {
        val cache = DepsDevCache(workspace.freshDirectory().toPath(), Duration.ofHours(1), FIXED_CLOCK)
        val thresholds = TrustThresholds(7, 30, 24, minScorecard)
        val client = { DepsDevClient(transport, "https://api.deps.dev", cache) }
        return TrustAuditor(client, setOf("com.acme"), thresholds, FIXED_CLOCK)
    }

    context("the trust assessment") {
        test("judges age and scorecard of every module") {
            val document = auditor(routedTransport(ROUTES)).audit(graphOf(MODULE), offline = false)

            document.status shouldBe DocumentStatus.COMPLETE
            document.findings.map { finding -> finding.ruleId } shouldBe listOf("FRESH_RELEASE", "LOW_SCORECARD")
        }

        test("never sends an internal module") {
            val transport = routedTransport(ROUTES)

            val document = auditor(transport).audit(graphOf(INTERNAL), offline = false)

            document.findings.shouldBeEmpty()

            transport.requests.shouldBeEmpty()
        }

        test("leaves out the scorecard when it is turned off") {
            val transport = routedTransport(ROUTES)

            auditor(transport, minScorecard = 0.0).audit(graphOf(MODULE), offline = false)

            transport.requests.size shouldBe 1
        }

        test("is incomplete offline, without asking anyone") {
            val transport = routedTransport(ROUTES)

            val document = auditor(transport).audit(graphOf(MODULE), offline = true)

            document.status shouldBe DocumentStatus.INCOMPLETE
            transport.requests.shouldBeEmpty()
        }

        test("is incomplete when deps.dev fails") {
            val transport = ScriptedTransport { request -> responseOf(500, request.uri.toString()) }

            val document = auditor(transport).audit(graphOf(MODULE), offline = false)

            document.status shouldBe DocumentStatus.INCOMPLETE
            document.reason.orEmpty() shouldContain "500"
        }

        test("is incomplete when a dependency does not resolve") {
            val document = auditor(routedTransport(ROUTES)).audit(graphOf(MODULE, unresolved = listOf("x:y:1")), false)

            document.status shouldBe DocumentStatus.INCOMPLETE
            document.reason.orEmpty() shouldContain "x:y:1"
        }
    }
})
