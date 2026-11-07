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
package com.davils.kreate.governance.fix

import com.davils.kreate.advisory.version.MavenVersion
import com.davils.kreate.governance.catalog.CatalogLibrary
import com.davils.kreate.governance.catalog.CatalogNames
import com.davils.kreate.governance.catalog.VersionCatalog
import com.davils.kreate.governance.policy.VersionKinds

internal class DependencyFixer(
    private val vulnerable: List<VulnerableVersion>,
    private val resolved: Map<String, String>
) {
    fun fix(text: String): FixResult {
        val aliases = VersionCatalog(text).libraries.map { library -> library.alias }
        val initial = FixResult(text, emptyList(), emptyList())
        return aliases.fold(initial) { result, alias -> fixed(result, alias) }
    }

    private fun fixed(result: FixResult, alias: String): FixResult {
        val catalog = VersionCatalog(result.text)
        val library = catalog.libraryNamed(alias) ?: return result
        val installed = catalog.effectiveVersion(library) ?: return result
        val target = targetOf(library, installed) ?: return result
        val sharers = catalog.sharersOf(library)
        if (sharers.isNotEmpty()) {
            val reason = "$alias shares version.ref ${library.versionRef} with ${sharers.joinToString()}; " +
                "raise it to $target by hand."
            return result.copy(skipped = result.skipped + reason)
        }
        val text = catalog.withVersion(library, target)
        return FixResult(text, result.changes + "$alias: $installed -> $target", result.skipped)
    }

    private fun targetOf(library: CatalogLibrary, installed: String): String? {
        val fixes = vulnerable.filter { entry -> entry.module == library.module && entry.version == installed }
        val fixed = fixes.map { entry -> entry.fixedVersion }.filter(CatalogNames::isExactVersion)
        val highest = fixed.maxByOrNull { version -> MavenVersion(version) }
        if (highest != null) return highest
        if (!VersionKinds.isDynamic(installed)) return null
        return resolved[library.module]?.takeIf(CatalogNames::isExactVersion)
    }
}
