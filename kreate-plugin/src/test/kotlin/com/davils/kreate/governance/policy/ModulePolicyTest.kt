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
package com.davils.kreate.governance.policy

import com.davils.kreate.dependency.ModuleCoordinate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private fun declared(
    module: String,
    requested: String = "1.0",
    configuration: String = "runtimeClasspath"
): DeclaredDependency {
    val (group, name) = module.split(':')
    return DeclaredDependency(configuration, group, name, requested, requested)
}

private fun rulesOf(
    settings: PolicySettings = PolicySettings(),
    modules: List<ModuleCoordinate> = emptyList(),
    declared: List<DeclaredDependency> = emptyList(),
    forced: List<ModuleCoordinate> = emptyList()
): List<String> {
    val findings = ModulePolicy(settings).findingsOf(modules, declared, forced)
    return findings.map { finding -> finding.ruleId }
}

class ModulePolicyTest : FunSpec({
    context("denied modules") {
        val settings = PolicySettings(denied = mapOf("log4j:log4j" to "End of life, use log4j-core."))

        test("are reported, also transitively, with the reason") {
            val log4j = ModuleCoordinate("log4j", "log4j", "1.2.17")

            val findings = ModulePolicy(settings).findingsOf(listOf(log4j), emptyList(), emptyList())

            val finding = findings.single()
            finding.ruleId shouldBe "DENIED_MODULE"
            finding.title shouldBe "log4j:log4j is denied: End of life, use log4j-core."
        }

        test("leave other modules alone") {
            val core = ModuleCoordinate("org.apache.logging.log4j", "log4j-core", "2.25.0")

            rulesOf(settings, modules = listOf(core)).shouldBeEmpty()
        }
    }

    context("the allowed list") {
        test("is not checked when empty") {
            rulesOf(declared = listOf(declared("org.example:lib"))).shouldBeEmpty()
        }

        test("reports a direct dependency it does not name") {
            val settings = PolicySettings(allowed = setOf("org.apache"))

            val dependencies = listOf(declared("org.apache.commons:commons-lang3"), declared("org.example:lib"))

            rulesOf(settings, declared = dependencies) shouldBe listOf("MODULE_NOT_ALLOWED")
        }
    }

    context("versions") {
        test("that are dynamic or snapshots are reported") {
            val dependencies = listOf(declared("org.example:a", "1.+"), declared("org.example:b", "1.0-SNAPSHOT"))

            rulesOf(declared = dependencies) shouldBe listOf("DYNAMIC_VERSION", "CHANGING_VERSION")
        }

        test("are accepted when the policy allows them") {
            val settings = PolicySettings(forbidDynamicVersions = false, forbidChangingVersions = false)
            val dependencies = listOf(declared("org.example:a", "1.+"), declared("org.example:b", "1.0-SNAPSHOT"))

            rulesOf(settings, declared = dependencies).shouldBeEmpty()
        }
    }

    context("test libraries") {
        test("on a production class path are reported once") {
            val dependencies = listOf(
                declared("io.mockk:mockk", configuration = "compileClasspath"),
                declared("io.mockk:mockk", configuration = "runtimeClasspath")
            )

            rulesOf(declared = dependencies) shouldBe listOf("TEST_LIBRARY_IN_PRODUCTION")
        }

        test("on a test class path are expected") {
            val junit = declared("org.junit.jupiter:junit-jupiter", configuration = "testRuntimeClasspath")

            rulesOf(declared = listOf(junit)).shouldBeEmpty()
        }
    }

    context("forced modules") {
        test("are reported") {
            val forced = listOf(ModuleCoordinate("org.example", "lib", "1.0"))

            rulesOf(forced = forced) shouldBe listOf("DEPENDENCY_OVERRIDE")
        }
    }
})
