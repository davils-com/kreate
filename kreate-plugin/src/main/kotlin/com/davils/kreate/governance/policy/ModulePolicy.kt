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

import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.rule.ruleFinding
import com.davils.kreate.dependency.ModuleCoordinate

private const val TEST_MARKER: String = "test"

internal class ModulePolicy(private val settings: PolicySettings) {
    fun findingsOf(
        modules: List<ModuleCoordinate>,
        declared: List<DeclaredDependency>,
        forced: List<ModuleCoordinate>
    ): List<Finding> {
        val direct = declared.distinctBy { dependency -> dependency.group to dependency.name }
        val denied = deniedOf(modules)
        val listed = notAllowedOf(direct)
        val versions = versionsOf(direct)
        return denied + listed + versions + testLibrariesOf(declared) + overridesOf(forced)
    }

    fun deniedOf(modules: List<ModuleCoordinate>): List<Finding> = modules.mapNotNull { module ->
        val entry = settings.denied.entries.firstOrNull { (pattern, _) -> matches(pattern, module) }
        entry?.let { (pattern, reason) ->
            ruleFinding("DENIED_MODULE", module, "${module.module} is denied: $reason", mapOf("pattern" to pattern))
        }
    }

    fun notAllowedOf(declared: List<DeclaredDependency>): List<Finding> {
        if (settings.allowed.isEmpty()) return emptyList()
        val unlisted = declared.filterNot { dependency -> isAllowed(dependency) }
        return unlisted.map { dependency ->
            val module = dependency.coordinate
            ruleFinding("MODULE_NOT_ALLOWED", module, "${module.module} is not on the allowed list")
        }
    }

    private fun versionsOf(declared: List<DeclaredDependency>): List<Finding> {
        val dynamic = declared.filter { dependency -> settings.forbidDynamicVersions && isDynamic(dependency) }
        val changing = declared.filter { dependency -> settings.forbidChangingVersions && isChanging(dependency) }
        return dynamic.map { dependency -> versionFinding("DYNAMIC_VERSION", dependency) } +
            changing.map { dependency -> versionFinding("CHANGING_VERSION", dependency) }
    }

    private fun testLibrariesOf(declared: List<DeclaredDependency>): List<Finding> {
        val production = declared.filterNot { dependency -> dependency.configuration.contains(TEST_MARKER, true) }
        val libraries = production.filter { dependency -> isTestLibrary(dependency) }
        return libraries.distinctBy { dependency -> dependency.group to dependency.name }.map { dependency ->
            val module = dependency.coordinate
            val title = "${module.module} is a test library on ${dependency.configuration}"
            ruleFinding("TEST_LIBRARY_IN_PRODUCTION", module, title, mapOf("configuration" to dependency.configuration))
        }
    }

    private fun overridesOf(forced: List<ModuleCoordinate>): List<Finding> = forced.distinct().map { module ->
        ruleFinding("DEPENDENCY_OVERRIDE", module, "${module.module} is forced to ${module.version}")
    }

    private fun versionFinding(rule: String, dependency: DeclaredDependency): Finding {
        val module = dependency.coordinate
        val title = "${module.module} is declared as ${dependency.requested}"
        return ruleFinding(rule, module, title, mapOf("requested" to dependency.requested))
    }

    private fun isAllowed(dependency: DeclaredDependency): Boolean =
        settings.allowed.any { pattern -> matchesModule(pattern, dependency.group, dependency.name) }

    private fun isTestLibrary(dependency: DeclaredDependency): Boolean =
        settings.testLibraries.any { pattern -> matchesModule(pattern, dependency.group, dependency.name) }

    private fun isDynamic(dependency: DeclaredDependency): Boolean = VersionKinds.isDynamic(dependency.requested)

    private fun isChanging(dependency: DeclaredDependency): Boolean = VersionKinds.isChanging(dependency.requested)

    private fun matches(pattern: String, module: ModuleCoordinate): Boolean =
        matchesModule(pattern, module.group, module.name)
}
