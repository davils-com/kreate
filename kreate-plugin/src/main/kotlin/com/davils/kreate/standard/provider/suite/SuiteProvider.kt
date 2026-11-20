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

import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.outcome.flatMap
import com.davils.kore.core.outcome.map
import com.davils.kore.core.text.SafeText
import com.davils.kreate.standard.fetch.FetchRequest
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardKind
import com.davils.kreate.standard.model.StandardSnapshot
import com.davils.kreate.standard.model.SuiteSource
import com.davils.kreate.standard.provider.JsonRecords.child
import com.davils.kreate.standard.provider.JsonRecords.text
import com.davils.kreate.standard.provider.ProviderContext
import com.davils.kreate.standard.provider.StandardProvider
import com.davils.kreate.standard.provider.document.Fingerprints
import com.davils.kreate.standard.provider.github.GitHubRequests
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.net.URI

private const val SHA256_PREFIX: String = "sha256:"

internal class SuiteProvider(private val context: ProviderContext) : StandardProvider {
    private val requests: GitHubRequests = GitHubRequests(context)

    override fun accepts(declaration: StandardDeclaration): Boolean = declaration.id.kind == StandardKind.SUITE

    override fun snapshotOf(declaration: StandardDeclaration): Outcome<StandardSnapshot, LookupFailure> {
        val suite = declaration.suite
            ?: return Outcome.Failure(LookupFailure.Unavailable("${declaration.id} declares no suite."))
        val project = context.projectDirectory
            ?: return Outcome.Failure(LookupFailure.Unavailable("The project of ${declaration.id} is unknown."))
        val vendored = suite.vendored.orEmpty()
        val tree = VendoredTree.read(project.resolve(vendored).normalize(), vendored)
        return tree.flatMap { found -> checked(declaration, suite, found) }
    }

    private fun checked(
        declaration: StandardDeclaration,
        suite: SuiteSource,
        tree: VendoredTree
    ): Outcome<StandardSnapshot, LookupFailure> {
        val index = indexOf(suite)
        return index.flatMap { found -> compared(declaration, suite, tree, found) }
    }

    private fun compared(
        declaration: StandardDeclaration,
        suite: SuiteSource,
        tree: VendoredTree,
        index: SuiteIndex
    ): Outcome<StandardSnapshot, LookupFailure> {
        val problem = index.problemOf(tree.files, originOf(suite))
        if (problem != null) return Outcome.Failure(LookupFailure.Mismatch(problem))
        return upstreamOf(declaration, suite, tree)
    }

    private fun indexOf(suite: SuiteSource): Outcome<SuiteIndex, LookupFailure> {
        val archive = suite.archive
        if (archive != null) return ArchiveIndexer(context).indexOf(archive, suite.sha256.orEmpty(), suite.root)
        return GitTreeIndexer(requests).indexOf(suite.github.orEmpty(), suite.commit.orEmpty(), suite.root)
    }

    private fun originOf(suite: SuiteSource): String {
        val archive = suite.archive ?: return "${suite.github} at ${suite.commit}"
        return "the archive $archive"
    }

    private fun upstreamOf(
        declaration: StandardDeclaration,
        suite: SuiteSource,
        tree: VendoredTree
    ): Outcome<StandardSnapshot, LookupFailure> {
        val base = StandardSnapshot(
            title = suite.github ?: titleOf(declaration, suite),
            revision = suite.sha256?.let { pinned -> SHA256_PREFIX + pinned },
            contentDigest = tree.digest
        )
        val watched = watchedOf(suite, base)
        val repository = suite.github ?: return watched
        return watched.flatMap { snapshot -> withHead(repository, snapshot) }
    }

    private fun titleOf(declaration: StandardDeclaration, suite: SuiteSource): String {
        val archive = suite.archive ?: return declaration.id.name
        val path = URI(archive).path.orEmpty()
        val name = path.substringAfterLast('/')
        return name.ifEmpty { declaration.id.name }
    }

    private fun watchedOf(suite: SuiteSource, snapshot: StandardSnapshot): Outcome<StandardSnapshot, LookupFailure> {
        val watch = suite.watch ?: return Outcome.Success(snapshot)
        val fetched = context.fetch(FetchRequest(URI(watch)))
        return fetched.map { document -> snapshot.copy(fingerprint = Fingerprints.fingerprintOf(document.bytes())) }
    }

    private fun withHead(repository: String, snapshot: StandardSnapshot): Outcome<StandardSnapshot, LookupFailure> {
        val commits = requests.jsonAt("repos/$repository/commits?per_page=1")
        return commits.map { listed ->
            val head = headOf(listed)
            val commit = head?.child("commit")
            val date = commit?.child("committer")?.text("date")
            snapshot.copy(revision = head?.text("sha")?.let(SafeText::text), published = date?.substringBefore('T'))
        }
    }

    private fun headOf(listed: JsonElement): JsonObject? {
        val array = listed as? JsonArray ?: return null
        return array.firstOrNull() as? JsonObject
    }
}
