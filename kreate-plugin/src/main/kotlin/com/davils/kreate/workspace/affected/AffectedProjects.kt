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
package com.davils.kreate.workspace.affected

import com.davils.kreate.workspace.model.WorkspaceMember

private val SHARED_FILES: Set<String> = setOf(
    "settings.gradle.kts",
    "settings.gradle",
    "build.gradle.kts",
    "build.gradle",
    "gradle.properties"
)

private val SHARED_DIRECTORIES: List<String> = listOf("gradle/", "buildSrc/", "build-logic/")

internal object AffectedProjects {
    fun affectedBy(members: List<WorkspaceMember>, changed: List<String>): List<String> {
        val all = members.map { member -> member.path }.sorted()
        if (changed.any(::isShared)) return all
        val owners = changed.mapNotNull { file -> ownerOf(members, file) }.toSet()
        val dependencies = members.associate { member -> member.path to member.projects }
        val affected = withDependents(owners, dependencies)
        return all.filter { path -> path in affected }
    }

    private fun isShared(file: String): Boolean = file in SHARED_FILES || SHARED_DIRECTORIES.any(file::startsWith)

    private fun ownerOf(members: List<WorkspaceMember>, file: String): String? {
        val owning = members.filter { member -> member.directory.isEmpty() || file.startsWith(member.directory + "/") }
        return owning.maxByOrNull { member -> member.directory.length }?.path
    }

    private fun withDependents(direct: Set<String>, dependencies: Map<String, Set<String>>): Set<String> {
        val affected = direct.toMutableSet()
        var grown = true
        while (grown) {
            val next = dependencies.filter { (path, targets) -> path !in affected && targets.any { it in affected } }
            grown = next.isNotEmpty()
            affected += next.keys
        }
        return affected
    }
}
