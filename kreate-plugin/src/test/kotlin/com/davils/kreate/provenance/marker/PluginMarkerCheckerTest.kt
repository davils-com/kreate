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
package com.davils.kreate.provenance.marker

import com.davils.kreate.dependency.ModuleCoordinate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private fun markerOf(pluginId: String, vararg implementations: String): PluginMarker {
    val marker = ModuleCoordinate(pluginId, "$pluginId.gradle.plugin", "1.0.0")
    val modules = implementations.map { group -> ModuleCoordinate(group, "plugin", "1.0.0") }
    return PluginMarker(pluginId, marker, modules)
}

private fun rulesOf(marker: PluginMarker, allow: Set<String> = emptySet()): List<String> {
    val findings = PluginMarkerChecker(allow).findingsOf(listOf(marker))
    return findings.map { finding -> finding.ruleId }
}

class PluginMarkerCheckerTest : FunSpec({
    context("a plugin marker") {
        test("that leads into the namespace of its id is accepted") {
            rulesOf(markerOf("org.jetbrains.kotlin.jvm", "org.jetbrains.kotlin")).shouldBeEmpty()
            rulesOf(markerOf("com.vanniktech.maven.publish", "com.vanniktech")).shouldBeEmpty()
        }

        test("that leads into another namespace is reported") {
            val findings = PluginMarkerChecker(emptySet()).findingsOf(listOf(markerOf("com.acme.build", "io.evil")))

            val finding = findings.single()

            finding.ruleId shouldBe "PLUGIN_MARKER_MISMATCH"
            finding.module shouldBe "com.acme.build:com.acme.build.gradle.plugin"
            finding.attributes["pluginId"] shouldBe "com.acme.build"
        }

        test("that leads to nothing is reported") {
            rulesOf(markerOf("com.acme.build")) shouldBe listOf("PLUGIN_MARKER_MISMATCH")
        }

        test("on a code hosting namespace must share the account as well") {
            rulesOf(markerOf("io.github.alice.plugin", "io.github.mallory")) shouldBe listOf("PLUGIN_MARKER_MISMATCH")
            rulesOf(markerOf("io.github.alice.plugin", "io.github.alice")).shouldBeEmpty()
        }

        test("that the build allows is accepted, by plugin id or by group") {
            val marker = markerOf("org.openapi.generator", "org.openapitools")

            rulesOf(marker, setOf("org.openapi.generator")).shouldBeEmpty()
            rulesOf(marker, setOf("org.openapitools")).shouldBeEmpty()
        }
    }
})
