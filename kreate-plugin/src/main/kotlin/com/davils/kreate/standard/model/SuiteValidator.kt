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
package com.davils.kreate.standard.model

import com.davils.kore.core.text.SafeText
import com.davils.kreate.archive.ArchivePathInspector

private val SHA256_HEX: Regex = Regex("^[0-9a-f]{64}$")

private val COMMIT_HEX: Regex = Regex("^[0-9a-f]{40}$")

internal object SuiteValidator {
    fun problemsOf(declaration: StandardDeclaration): List<String> {
        val id = declaration.id
        val isSuite = id.kind == StandardKind.SUITE
        val suite = declaration.suite ?: return listOfNotNull("$id needs a suite { }.".takeIf { isSuite })
        if (!isSuite) return listOf("$id takes no suite; only suite() does.")
        val locations = listOfNotNull(
            pathProblem(id, "vendored", suite.vendored, isRequired = true),
            pathProblem(id, "root", suite.root, isRequired = false),
            suite.watch?.let { watch -> addressProblem(id, "watch", watch) }
        )
        return originProblems(id, suite) + locations
    }

    private fun originProblems(id: StandardId, suite: SuiteSource): List<String> {
        val hasArchive = suite.archive != null
        val hasRepository = suite.github != null
        if (hasArchive && hasRepository) return listOf("$id takes an archive or a github repository, not both.")
        if (hasArchive) return archiveProblems(id, suite)
        if (hasRepository) return githubProblems(id, suite)
        return listOf("$id needs an archive with its sha256 or a github repository with its commit.")
    }

    private fun archiveProblems(id: StandardId, suite: SuiteSource): List<String> = listOfNotNull(
        suite.archive?.let { archive -> addressProblem(id, "archive", archive) },
        "$id needs the sha256 of its archive as 64 hexadecimal digits.".takeUnless { isHex(SHA256_HEX, suite.sha256) },
        "$id takes a commit only with a github repository.".takeIf { suite.commit != null }
    )

    private fun githubProblems(id: StandardId, suite: SuiteSource): List<String> {
        val repository = suite.github.orEmpty()
        val isRepository = StandardIdGrammar.isValid(StandardKind.GITHUB, repository)
        return listOfNotNull(
            "$id has the repository ${SafeText.quoted(repository)}, which is not of the form owner/name."
                .takeUnless { isRepository },
            "$id needs the commit it vendors as 40 hexadecimal digits.".takeUnless { isHex(COMMIT_HEX, suite.commit) },
            "$id takes a sha256 only with an archive.".takeIf { suite.sha256 != null }
        )
    }

    private fun isHex(pattern: Regex, text: String?): Boolean = text != null && pattern.matches(text)

    private fun pathProblem(id: StandardId, name: String, path: String?, isRequired: Boolean): String? {
        val missing = "$id needs $name, the directory of the project that holds the suite."
        if (path == null) return missing.takeIf { isRequired }
        val isBlank = isRequired && path.isBlank()
        if (!isBlank && ArchivePathInspector.inspect(path) == null) return null
        return "$id has the $name ${SafeText.quoted(path)}, which is no relative path inside the project."
    }

    private fun addressProblem(id: StandardId, name: String, url: String): String? {
        if (WatchableAddresses.isWatchable(url)) return null
        return "$id has the $name ${SafeText.quoted(url)}, which is neither HTTPS nor a loopback address."
    }
}
