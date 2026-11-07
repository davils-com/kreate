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
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.dependency.ResolvedGraph
import com.davils.kreate.net.FIXED_CLOCK
import com.davils.kreate.net.ScriptedTransport
import com.davils.kreate.net.responseOf
import com.davils.kreate.provenance.confusion.MavenMetadataClient
import com.davils.kreate.provenance.marker.PluginMarker
import com.davils.kreate.provenance.marker.PluginMarkerChecker
import com.davils.kreate.provenance.typosquat.TyposquatDetector
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private val INTERNAL: ModuleCoordinate = ModuleCoordinate("com.acme.secret", "billing", "1.0.0")

private val PUBLIC: ModuleCoordinate = ModuleCoordinate("org.example", "lib", "99.0.0")

private const val METADATA: String = "<metadata><versioning><release>9.9.9</release></versioning></metadata>"

private fun graphOf(vararg modules: ModuleCoordinate) = listOf(ResolvedGraph(modules.toList(), emptyList()))

private fun FindingDocument.rules(): List<String> = findings.map { finding -> finding.ruleId }

private fun publishing(status: Int): ScriptedTransport =
    ScriptedTransport { request -> responseOf(status, request.uri.toString(), body = METADATA.toByteArray()) }

private fun auditor(transport: ScriptedTransport?): ConfusionAuditor {
    val lookup = transport?.let { scripted -> { MavenMetadataClient(scripted, "https://repo.example") } }
    val typosquat = TyposquatDetector(setOf("com.google.guava:guava"), emptySet())
    return ConfusionAuditor(typosquat, PluginMarkerChecker(emptySet()), lookup, setOf("com.acme"), FIXED_CLOCK)
}

class ConfusionAuditorTest : FunSpec({
    context("the confusion check") {
        test("works without the network") {
            val marker = PluginMarker("com.acme.build", INTERNAL, emptyList())

            val document = auditor(null).audit(graphOf(PUBLIC), listOf(marker), offline = true)

            document.status shouldBe DocumentStatus.COMPLETE
            document.rules() shouldBe listOf("SUSPICIOUS_VERSION", "PLUGIN_MARKER_MISMATCH")
        }

        test("reports an internal module the public repository also publishes") {
            val document = auditor(publishing(200)).audit(graphOf(INTERNAL), emptyList(), offline = false)

            val finding = document.findings.single()
            finding.ruleId shouldBe "DEPENDENCY_CONFUSION"
            finding.attributes["publicVersion"] shouldBe "9.9.9"
        }

        test("asks the public repository only about internal modules") {
            val transport = publishing(404)

            auditor(transport).audit(graphOf(INTERNAL, PUBLIC.copy(version = "1.0.0")), emptyList(), offline = false)

            val paths = transport.requests.map { request -> request.uri.path }
            paths shouldBe listOf("/com/acme/secret/billing/maven-metadata.xml")
        }

        test("finds nothing when the public repository does not know the module") {
            val document = auditor(publishing(404)).audit(graphOf(INTERNAL), emptyList(), offline = false)

            document.findings.shouldBeEmpty()
        }

        test("is incomplete offline, but keeps what it found locally") {
            val document = auditor(publishing(200)).audit(graphOf(INTERNAL, PUBLIC), emptyList(), offline = true)

            document.status shouldBe DocumentStatus.INCOMPLETE
            document.rules() shouldBe listOf("SUSPICIOUS_VERSION")
        }

        test("is incomplete when the public repository fails") {
            val document = auditor(publishing(500)).audit(graphOf(INTERNAL), emptyList(), offline = false)

            document.status shouldBe DocumentStatus.INCOMPLETE
        }
    }
})
