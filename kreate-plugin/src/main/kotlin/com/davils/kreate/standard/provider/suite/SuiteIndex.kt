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
package com.davils.kreate.standard.provider.suite

internal class SuiteIndex(val digests: Map<String, String>, val isGitBlob: Boolean) {
    fun problemOf(files: List<VendoredFile>, origin: String): String? {
        val missing = files.filter { file -> file.path !in digests }
        val differing = files.filter { file -> file.path in digests && digests[file.path] != digestOf(file) }
        val problems = listOfNotNull(
            listed(missing, "is not in $origin", "are not in $origin"),
            listed(differing, "differs from $origin", "differ from $origin")
        )
        if (problems.isEmpty()) return null
        return problems.joinToString(" ")
    }

    private fun digestOf(file: VendoredFile): String = when (isGitBlob) {
        true -> file.gitBlob
        false -> file.sha256
    }

    private fun listed(files: List<VendoredFile>, single: String, plural: String): String? {
        val first = files.firstOrNull() ?: return null
        if (files.size == 1) return "The vendored file ${first.path} $single."
        return "${files.size} vendored files $plural, such as ${first.path}."
    }

    companion object {
        fun rooted(paths: Map<String, String>, root: String?, isGitBlob: Boolean): SuiteIndex {
            val prefix = prefixOf(root ?: commonRootOf(paths.keys))
            val inside = paths.filterKeys { path -> path.startsWith(prefix) }
            val relative = inside.mapKeys { (path, _) -> path.removePrefix(prefix) }
            return SuiteIndex(relative.filterKeys { path -> path.isNotEmpty() }, isGitBlob)
        }

        private fun prefixOf(root: String): String {
            val trimmed = root.trim('/')
            if (trimmed.isEmpty()) return ""
            return "$trimmed/"
        }

        private fun commonRootOf(paths: Set<String>): String {
            val firsts = paths.map { path -> path.substringBefore('/', missingDelimiterValue = "") }
            val distinct = firsts.distinct()
            return distinct.singleOrNull().orEmpty()
        }
    }
}
