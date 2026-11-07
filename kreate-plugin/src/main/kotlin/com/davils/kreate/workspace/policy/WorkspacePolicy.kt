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
import com.davils.kreate.assurance.rule.ruleFinding
import com.davils.kreate.workspace.model.WorkspaceMember

private const val ARROW: String = "->"

internal class WorkspacePolicy(private val settings: WorkspaceSettings) {
    fun check(members: List<WorkspaceMember>, expected: Set<String>): List<Finding> =
        missingOf(members, expected) + versionsOf(members) + cyclesOf(members) + forbiddenOf(members)

    private fun missingOf(members: List<WorkspaceMember>, expected: Set<String>): List<Finding> {
        val present = members.map { member -> member.path }.toSet()
        return (expected - present).sorted().map { path ->
            val title = "$path does not share its dependencies with the workspace"
            val attributes = mapOf("project" to path, REPORT_ONLY_ATTRIBUTE to "true")
            ruleFinding("WORKSPACE_MEMBER_MISSING", null, title, attributes)
        }
    }

    private fun versionsOf(members: List<WorkspaceMember>): List<Finding> {
        if (!settings.alignVersions) return emptyList()
        val usages = members.flatMap { member ->
            member.modules.map { (module, version) -> ModuleUsage(module, version, member.path) }
        }
        val byModule = usages.groupBy { usage -> usage.module }
        val misaligned = byModule.filterValues { list -> list.distinctBy { usage -> usage.version }.size > 1 }
        return misaligned.toSortedMap().map { (module, list) -> mismatchOf(module, list) }
    }

    private fun mismatchOf(module: String, usages: List<ModuleUsage>): Finding {
        val versions = usages.groupBy({ usage -> usage.version }, { usage -> usage.project })
        val listing = versions.toSortedMap().map { (version, paths) -> "$version (${paths.sorted().joinToString()})" }
        val title = "$module resolves as ${listing.joinToString("; ")}"
        return ruleFinding("WORKSPACE_VERSION_MISMATCH", null, title, mapOf("module" to module))
    }

    private fun cyclesOf(members: List<WorkspaceMember>): List<Finding> {
        val graph = members.associate { member -> member.path to member.projects.filter { it != member.path } }
        val cycles = StronglyConnected.componentsOf(graph).filter { component -> component.size > 1 }
        return cycles.map { component ->
            val cycle = component.sorted()
            val title = "${cycle.joinToString(" -> ")} depend on each other"
            ruleFinding("DEPENDENCY_CYCLE", null, title, mapOf("projects" to cycle.joinToString()))
        }
    }

    private fun forbiddenOf(members: List<WorkspaceMember>): List<Finding> {
        val rules = settings.forbidden.mapNotNull { rule -> ruleOf(rule) }
        val edges = members.flatMap { member -> member.projects.map { target -> member.path to target } }
        return edges.flatMap { (from, to) -> violationsOf(rules, from, to) }
    }

    private fun violationsOf(rules: List<Pair<String, String>>, from: String, to: String): List<Finding> {
        val matching = rules.filter { (source, target) ->
            ProjectPattern.matches(source, from) && ProjectPattern.matches(target, to)
        }
        return matching.map { (source, target) ->
            val title = "$from depends on $to, which $source -> $target forbids"
            ruleFinding("FORBIDDEN_DEPENDENCY", null, title, mapOf("from" to from, "to" to to))
        }
    }

    private fun ruleOf(rule: String): Pair<String, String>? {
        if (!rule.contains(ARROW)) return null
        return rule.substringBefore(ARROW).trim() to rule.substringAfter(ARROW).trim()
    }
}
