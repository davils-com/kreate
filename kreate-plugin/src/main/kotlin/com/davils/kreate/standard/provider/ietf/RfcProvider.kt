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
package com.davils.kreate.standard.provider.ietf

import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.outcome.flatMap
import com.davils.kore.core.outcome.map
import com.davils.kreate.standard.fetch.FetchRequest
import com.davils.kreate.standard.model.ErratumState
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardKind
import com.davils.kreate.standard.model.StandardSnapshot
import com.davils.kreate.standard.provider.JsonRecords
import com.davils.kreate.standard.provider.JsonRecords.text
import com.davils.kreate.standard.provider.JsonRecords.texts
import com.davils.kreate.standard.provider.ProviderContext
import com.davils.kreate.standard.provider.StandardProvider
import kotlinx.serialization.json.JsonObject

private const val MAX_RECORD_BYTES: Int = 1024 * 1024

internal class RfcProvider(private val context: ProviderContext) : StandardProvider {
    private val errata: Lazy<Outcome<ErrataIndex, LookupFailure>> = lazy { ErrataIndex.load(context) }

    override fun accepts(declaration: StandardDeclaration): Boolean = declaration.id.kind == StandardKind.RFC

    override fun snapshotOf(declaration: StandardDeclaration): Outcome<StandardSnapshot, LookupFailure> {
        val number = declaration.id.name
        val uri = context.endpoints.resolve(context.endpoints.rfcEditor, "rfc/rfc$number.json")
        val record = context.fetch(FetchRequest(uri, MAX_RECORD_BYTES))
        return record.flatMap { document -> snapshotOf(number, document.text()) }
    }

    private fun snapshotOf(number: String, text: String): Outcome<StandardSnapshot, LookupFailure> {
        val json = JsonRecords.objectOf(text)
            ?: return Outcome.Failure(LookupFailure.Unavailable("The record of RFC $number is no JSON."))
        val index = errata.value
        return index.map { found -> snapshotOf(json, found.errataOf("RFC$number")) }
    }

    private fun snapshotOf(json: JsonObject, errata: Map<String, ErratumState>): StandardSnapshot {
        val replacedBy = json.texts("obsoleted_by")
        return StandardSnapshot(
            title = json.text("title").orEmpty(),
            status = json.text("status"),
            published = json.text("pub_date"),
            withdrawn = replacedBy.isNotEmpty(),
            replacedBy = replacedBy,
            updatedBy = json.texts("updated_by"),
            errata = errata
        )
    }
}
