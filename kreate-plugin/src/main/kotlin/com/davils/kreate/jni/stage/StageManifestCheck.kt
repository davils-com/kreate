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

package com.davils.kreate.jni.stage

import com.davils.kore.core.text.SafeText
import com.davils.kreate.digest.Sha256
import java.io.File

internal object StageManifestCheck {
    fun review(manifest: StageManifest, expectation: StageExpectation, directory: File): StageReview {
        val identity = identityProblems(manifest, expectation)
        val files = fileProblems(manifest, directory)
        val revision = revisionFinding(manifest.revision, expectation)
        val revisionProblems = listOfNotNull(revision.takeIf { expectation.isRelease })
        val revisionWarnings = listOfNotNull(revision.takeIf { !expectation.isRelease })
        val warnings = revisionWarnings + unknownRevision(manifest, expectation)
        return StageReview(problems = identity + files + revisionProblems, warnings = warnings)
    }

    private fun identityProblems(manifest: StageManifest, expectation: StageExpectation): List<String> = listOfNotNull(
        mismatch("platform", manifest.platform, expectation.platform),
        mismatch("group", manifest.group, expectation.group),
        mismatch("artifact", manifest.artifact, expectation.artifact),
        mismatch("version", manifest.version, expectation.version)
    )

    private fun mismatch(field: String, staged: String, expected: String): String? {
        if (staged == expected) return null
        val shownStaged = SafeText.quoted(staged)
        return "was staged for the $field $shownStaged, but this build publishes ${SafeText.quoted(expected)}"
    }

    private fun revisionFinding(staged: String?, expectation: StageExpectation): String? {
        val expected = expectation.revision ?: return null
        if (staged == null || staged == expected) return null
        val shownStaged = SafeText.quoted(staged)
        return "was built from the commit $shownStaged, but this build is ${SafeText.quoted(expected)}"
    }

    private fun unknownRevision(manifest: StageManifest, expectation: StageExpectation): List<String> {
        val staged = manifest.revision
        val hasBoth = staged != null && expectation.revision != null
        if (hasBoth) return emptyList()
        return listOf(
            "cannot be matched to a commit, because Git could not name the commit of the staging or of this build"
        )
    }

    private fun fileProblems(manifest: StageManifest, directory: File): List<String> {
        val entries = entriesOf(directory)
        val directories = entries.filter { entry -> entry.isDirectory }
        val present = entries.filter { entry -> entry.isFile && entry.name != STAGE_MANIFEST_FILE_NAME }
        val presentNames = present.map { file -> file.name }.toSet()
        val listedNames = manifest.digests.keys
        val unlisted = (presentNames - listedNames).sorted()
        val missing = (listedNames - presentNames).sorted()
        val changed = present.filter { file -> isChanged(file, manifest.digests) }
        val nested = directories.map { entry ->
            "holds the directory ${SafeText.quoted(entry.name)}, which is never staged"
        }
        return nested +
            unlisted.map { name -> "holds ${SafeText.quoted(name)}, which its manifest does not list" } +
            missing.map { name -> "lists ${SafeText.quoted(name)}, which is not there" } +
            changed.map { file -> "holds ${SafeText.quoted(file.name)} with a SHA-256 its manifest does not give" }
    }

    private fun isChanged(file: File, digests: Map<String, String>): Boolean {
        val expected = digests[file.name] ?: return false
        return Sha256.hexOf(file.toPath()) != expected
    }

    private fun entriesOf(directory: File): List<File> {
        val entries = directory.listFiles() ?: return emptyList()
        return entries.toList()
    }
}
