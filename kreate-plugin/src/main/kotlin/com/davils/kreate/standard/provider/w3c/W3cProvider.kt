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
package com.davils.kreate.standard.provider.w3c

import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.outcome.flatMap
import com.davils.kore.core.outcome.map
import com.davils.kreate.standard.fetch.FetchRequest
import com.davils.kreate.standard.model.ErrataAddress
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardId
import com.davils.kreate.standard.model.StandardKind
import com.davils.kreate.standard.model.StandardSnapshot
import com.davils.kreate.standard.provider.JsonRecords
import com.davils.kreate.standard.provider.ProviderContext
import com.davils.kreate.standard.provider.StandardProvider
import kotlinx.serialization.json.JsonObject
import java.net.URI

internal class W3cProvider(
    private val context: ProviderContext,
    declarations: List<StandardDeclaration> = emptyList()
) : StandardProvider {
    private val declared: Set<StandardId> = declarations.map { declaration -> declaration.id }.toSet()

    private val errata: W3cErrata = W3cErrata(context)

    override fun accepts(declaration: StandardDeclaration): Boolean = declaration.id.kind == StandardKind.W3C

    override fun snapshotOf(declaration: StandardDeclaration): Outcome<StandardSnapshot, LookupFailure> {
        val name = declaration.id.name
        val uri = context.endpoints.resolve(context.endpoints.w3cApi, "specifications/$name")
        val record = jsonAt(uri, "the W3C specification $name")
        return record.flatMap { specification -> snapshotOf(declaration, specification) }
    }

    private fun snapshotOf(
        declaration: StandardDeclaration,
        specification: JsonObject
    ): Outcome<StandardSnapshot, LookupFailure> {
        val snapshot = withSeries(declaration.id.name, specification)
        return snapshot.flatMap { found -> withErrata(declaration, specification, found) }
    }

    private fun withSeries(name: String, specification: JsonObject): Outcome<StandardSnapshot, LookupFailure> {
        val snapshot = W3cRecords.snapshotOf(specification)
        val series = W3cRecords.seriesOf(specification) ?: return Outcome.Success(snapshot)
        val seriesUri = context.endpoints.resolve(context.endpoints.w3cApi, series)
        val seriesRecord = jsonAt(seriesUri, "the W3C series of $name")
        return seriesRecord.map { found ->
            val current = W3cRecords.currentOf(found)
            val newer = current?.takeIf { shortname -> !shortname.equals(name, ignoreCase = true) }
            val successor = newer?.let { shortname -> StandardId(StandardKind.W3C, shortname.lowercase()) }
            val undeclared = successor?.takeIf { id -> id !in declared }
            snapshot.copy(updatedBy = listOfNotNull(undeclared?.toString()))
        }
    }

    private fun withErrata(
        declaration: StandardDeclaration,
        specification: JsonObject,
        snapshot: StandardSnapshot
    ): Outcome<StandardSnapshot, LookupFailure> {
        val address = errataAddressOf(declaration, specification)
        return address.flatMap { found ->
            val record = found?.let(errata::read) ?: Outcome.Success(ErrataRecord(emptyMap()))
            record.map { read -> snapshot.copy(errata = read.errata, fingerprint = read.fingerprint) }
        }
    }

    private fun errataAddressOf(
        declaration: StandardDeclaration,
        specification: JsonObject
    ): Outcome<ErrataAddress?, LookupFailure> {
        val declared = declaration.errata
        if (declared != null) return Outcome.Success(ErrataAddress.parse(declared))
        val version = W3cRecords.latestVersionOf(specification) ?: return Outcome.Success(null)
        val uri = context.endpoints.resolve(context.endpoints.w3cApi, version)
        val record = jsonAt(uri, "the latest version of the W3C specification ${declaration.id.name}")
        return record.map { found ->
            val link = W3cRecords.errataOf(found)
            link?.let { text -> ErrataAddress.parse(text, upgradesPlainHttp = true) }
        }
    }

    private fun jsonAt(uri: URI, what: String): Outcome<JsonObject, LookupFailure> {
        val fetched = context.fetch(FetchRequest(uri))
        return fetched.flatMap { document ->
            val json = JsonRecords.objectOf(document.text())
            json?.let { found -> Outcome.Success(found) }
                ?: Outcome.Failure(LookupFailure.Unavailable("The record of $what is no JSON."))
        }
    }
}
