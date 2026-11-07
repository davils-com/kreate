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
package com.davils.kreate.workspace.policy

import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.policy.REPORT_ONLY_ATTRIBUTE
import com.davils.kreate.workspace.affected.AffectedProjects
import com.davils.kreate.workspace.model.WorkspaceMember
import com.davils.kreate.workspace.model.WorkspaceMemberJson
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private fun member(path: String, vararg projects: String, modules: Map<String, String> = emptyMap()) =
    WorkspaceMember(path, path.removePrefix(":").replace(':', '/'), projects.toSet(), modules)

private val MISALIGNED: List<WorkspaceMember> = listOf(
    member(":a", modules = mapOf("g:x" to "1.0")),
    member(":b", modules = mapOf("g:x" to "2.0"))
)

private fun findingsOf(
    members: List<WorkspaceMember>,
    forbidden: Set<String> = emptySet(),
    alignVersions: Boolean = true,
    expected: Set<String> = emptySet()
): List<Finding> {
    val policy = WorkspacePolicy(WorkspaceSettings(alignVersions, forbidden))
    return policy.check(members, expected)
}

private fun rulesOf(members: List<WorkspaceMember>, forbidden: Set<String> = emptySet()): List<String> {
    val expected = members.map { member -> member.path }.toSet()
    return findingsOf(members, forbidden, expected = expected).map { finding -> finding.ruleId }
}

class WorkspacePolicyTest : FunSpec({
    context("the workspace") {
        test("that is consistent passes") {
            rulesOf(listOf(member(":app", ":core"), member(":core"))).shouldBeEmpty()
        }

        test("reports a module in different versions with the projects using each") {
            val finding = findingsOf(MISALIGNED).single()

            finding.ruleId shouldBe "WORKSPACE_VERSION_MISMATCH"
            finding.title shouldBe "g:x resolves as 1.0 (:a); 2.0 (:b)"
        }

        test("leaves versions alone when asked to") {
            findingsOf(MISALIGNED, alignVersions = false).shouldBeEmpty()
        }

        test("reports a cycle once") {
            val cycle = listOf(member(":a", ":b"), member(":b", ":c"), member(":c", ":a"))

            rulesOf(cycle) shouldBe listOf("DEPENDENCY_CYCLE")
        }

        test("reports a forbidden dependency by pattern") {
            val members = listOf(member(":core:model", ":app:ui"), member(":app:ui"))

            rulesOf(members, setOf(":core:** -> :app:**")) shouldBe listOf("FORBIDDEN_DEPENDENCY")
            rulesOf(members, setOf(":core:* -> :lib:*")).shouldBeEmpty()
        }

        test("reports a member that shared nothing, without blocking") {
            val finding = findingsOf(listOf(member(":a")), expected = setOf(":a", ":b")).single()

            finding.ruleId shouldBe "WORKSPACE_MEMBER_MISSING"
            (REPORT_ONLY_ATTRIBUTE in finding.attributes) shouldBe true
        }
    }

    context("a pattern") {
        test("matches within a segment with * and across segments with **") {
            ProjectPattern.matches(":core:*", ":core:model") shouldBe true
            ProjectPattern.matches(":core:*", ":core:model:api") shouldBe false
            ProjectPattern.matches(":core:**", ":core:model:api") shouldBe true
            ProjectPattern.matches(":core", ":corex") shouldBe false
        }
    }

    context("the affected projects") {
        val members = listOf(member(":"), member(":app", ":core"), member(":core"), member(":tool"))

        test("are the changed project and its dependents") {
            AffectedProjects.affectedBy(members, listOf("core/src/main/kotlin/A.kt")) shouldBe listOf(":app", ":core")
        }

        test("are all projects after a change of shared build files") {
            AffectedProjects.affectedBy(members, listOf("gradle/libs.versions.toml")).size shouldBe 4
        }

        test("are the root project for a file outside every subproject") {
            AffectedProjects.affectedBy(members, listOf("README.md")) shouldBe listOf(":")
        }
    }

    context("a member description") {
        test("survives a round trip and refuses what is not one") {
            val described = member(":app", ":core", modules = mapOf("g:x" to "1.0"))

            WorkspaceMemberJson.parse(WorkspaceMemberJson.render(described)) shouldBe described
            WorkspaceMemberJson.parse("[1]") shouldBe null
            WorkspaceMemberJson.parse("not json") shouldBe null
        }
    }
})
