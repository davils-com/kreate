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
package com.davils.kreate.standard.provider.iso

import com.davils.kore.core.outcome.Outcome
import com.davils.kreate.standard.fetch.FetchRequest
import com.davils.kreate.standard.fetch.FetchedDocument
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardIdGrammar
import com.davils.kreate.standard.model.StandardKind
import com.davils.kreate.standard.model.StandardSnapshot
import com.davils.kreate.standard.provider.ProviderContext
import com.davils.kreate.standard.provider.StandardProvider
import java.io.IOException

private const val MAX_EXPORT_BYTES: Int = 512 * 1024 * 1024

private const val EXPORT_PATH: String = "iso_deliverables_metadata/json/iso_deliverables_metadata.jsonl"

internal class IsoProvider(
    private val context: ProviderContext,
    declarations: List<StandardDeclaration>
) : StandardProvider {
    private val references: Set<String> = referencesOf(declarations)

    private val records: Lazy<Outcome<Map<String, List<IsoRecord>>, LookupFailure>> = lazy { load() }

    override fun accepts(declaration: StandardDeclaration): Boolean = declaration.id.kind == StandardKind.ISO

    override fun snapshotOf(declaration: StandardDeclaration): Outcome<StandardSnapshot, LookupFailure> {
        val reference = StandardIdGrammar.isoReferenceOf(declaration.id)
        return when (val loaded = records.value) {
            is Outcome.Failure -> loaded
            is Outcome.Success -> snapshotOf(reference, loaded.value[reference].orEmpty())
        }
    }

    private fun snapshotOf(reference: String, matching: List<IsoRecord>): Outcome<StandardSnapshot, LookupFailure> {
        val snapshot = IsoDeliverables.snapshotOf(reference, matching)
            ?: return Outcome.Failure(LookupFailure.Unknown("ISO publishes no edition of $reference."))
        return Outcome.Success(snapshot)
    }

    private fun load(): Outcome<Map<String, List<IsoRecord>>, LookupFailure> {
        val uri = context.endpoints.resolve(context.endpoints.isoOpenData, EXPORT_PATH)
        return when (val fetched = context.fetch(FetchRequest(uri, MAX_EXPORT_BYTES, isBulk = true))) {
            is Outcome.Failure -> fetched
            is Outcome.Success -> read(fetched.value)
        }
    }

    private fun read(document: FetchedDocument): Outcome<Map<String, List<IsoRecord>>, LookupFailure> = try {
        val markers = references.associateBy { reference -> IsoDeliverables.markerOf(reference) }
        val found = document.reader().useLines { lines -> recordsIn(lines, markers) }
        Outcome.Success(found)
    } catch (cause: IOException) {
        Outcome.Failure(LookupFailure.Unavailable("The open data of ISO could not be read: ${cause.message}"))
    }

    private fun recordsIn(lines: Sequence<String>, markers: Map<String, String>): Map<String, List<IsoRecord>> {
        val matched = lines.flatMap { line -> matchesIn(line, markers) }
        val pairs = matched.mapNotNull { (reference, line) -> recordFor(reference, line) }
        return pairs.groupBy({ pair -> pair.first }, { pair -> pair.second })
    }

    private fun recordFor(reference: String, line: String): Pair<String, IsoRecord>? {
        val record = IsoDeliverables.recordOf(line) ?: return null
        return reference to record
    }

    private fun referencesOf(declarations: List<StandardDeclaration>): Set<String> {
        val iso = declarations.filter { declaration -> declaration.id.kind == StandardKind.ISO }
        val references = iso.map { declaration -> StandardIdGrammar.isoReferenceOf(declaration.id) }
        return references.toSet()
    }

    private fun matchesIn(line: String, markers: Map<String, String>): Sequence<Pair<String, String>> {
        val hits = markers.filterKeys { marker -> line.contains(marker) }
        return hits.values.asSequence().map { reference -> reference to line }
    }
}
