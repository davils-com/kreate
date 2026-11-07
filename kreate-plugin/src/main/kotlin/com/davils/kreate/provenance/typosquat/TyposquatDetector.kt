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
package com.davils.kreate.provenance.typosquat

import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.rule.ruleFinding
import com.davils.kreate.dependency.ModuleCoordinate

private const val NEAR: Int = 1

private const val FAR: Int = 2

private const val MIN_NEAR_LENGTH: Int = 4

private const val MIN_FAR_LENGTH: Int = 6

private const val MIN_ARTIFACT_LENGTH: Int = 5

private val SEPARATORS: Regex = Regex("[-_.]")

private val GENERIC_ARTIFACTS: Set<String> = setOf(
    "annotations", "api", "bom", "client", "common", "core", "gradle", "java", "kotlin", "runtime", "server", "utils"
)

internal class TyposquatDetector(popular: Set<String>, private val allow: Set<String>) {
    private val popularCoordinates: Set<String> = popular

    private val owners: Set<String> = popular.map { coordinate ->
        ownerOf(coordinate.substringBefore(':'))
    }.toSet()

    private val artifacts: Map<String, String> = popular.associateBy { coordinate ->
        normalised(artifactOf(coordinate))
    }

    fun findingsOf(modules: List<ModuleCoordinate>): List<Finding> {
        val candidates = modules.filterNot { module -> isAccepted(module) }
        return candidates.mapNotNull { module -> groupFindingOf(module) ?: artifactFindingOf(module) }
    }

    private fun isAccepted(module: ModuleCoordinate): Boolean {
        if (module.module in popularCoordinates) return true
        if (module.module in allow || module.group in allow) return true
        return ownerOf(module.group) in owners
    }

    private fun groupFindingOf(module: ModuleCoordinate): Finding? {
        val owner = ownerOf(module.group)
        val name = owner.substringAfter('.')
        val match = owners.firstOrNull { popular -> isLookAlike(owner, popular, name) } ?: return null
        val title = "The group ${module.group} looks like $match"
        return ruleFinding("TYPOSQUAT_GROUP", module, title, mapOf("imitates" to match))
    }

    private fun isLookAlike(owner: String, popular: String, name: String): Boolean {
        val popularName = popular.substringAfter('.')
        if (name == popularName && owner != popular) return true
        if (normalised(owner) == normalised(popular)) return true
        val distance = boundedEditDistance(owner, popular, FAR)
        if (distance == NEAR) return name.length >= MIN_NEAR_LENGTH
        if (distance == FAR) return name.length >= MIN_FAR_LENGTH
        return false
    }

    private fun artifactFindingOf(module: ModuleCoordinate): Finding? {
        val artifact = normalised(module.name)
        if (artifact.length < MIN_ARTIFACT_LENGTH || module.name in GENERIC_ARTIFACTS) return null
        val match = artifacts.entries.firstOrNull { (name, _) -> isNearArtifact(artifact, name) } ?: return null
        val title = "${module.name} carries the name of ${match.value} under a foreign group"
        return ruleFinding("TYPOSQUAT_ARTIFACT", module, title, mapOf("imitates" to match.value))
    }

    private fun isNearArtifact(artifact: String, popular: String): Boolean {
        if (popular in GENERIC_ARTIFACTS || popular.length < MIN_ARTIFACT_LENGTH) return false
        return boundedEditDistance(artifact, popular, NEAR) <= NEAR
    }

    private fun artifactOf(coordinate: String): String = coordinate.substringAfter(':')

    private fun normalised(name: String): String = name.lowercase().replace(SEPARATORS, "")
}
