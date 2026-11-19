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
import com.davils.kreate.standard.fetch.FetchRequest
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardKind
import com.davils.kreate.standard.model.StandardSnapshot
import com.davils.kreate.standard.provider.JsonRecords
import com.davils.kreate.standard.provider.JsonRecords.text
import com.davils.kreate.standard.provider.ProviderContext
import com.davils.kreate.standard.provider.StandardProvider

private const val MAX_RECORD_BYTES: Int = 4 * 1024 * 1024

private val CLOSED_STATES: Set<String> = setOf("RFC", "Replaced", "Withdrawn by Submitter", "Withdrawn by IETF")

internal class DraftProvider(private val context: ProviderContext) : StandardProvider {
    override fun accepts(declaration: StandardDeclaration): Boolean = declaration.id.kind == StandardKind.DRAFT

    override fun snapshotOf(declaration: StandardDeclaration): Outcome<StandardSnapshot, LookupFailure> {
        val name = declaration.id.name
        val uri = context.endpoints.resolve(context.endpoints.datatracker, "doc/$name/doc.json")
        val record = context.fetch(FetchRequest(uri, MAX_RECORD_BYTES))
        return record.flatMap { document -> snapshotOf(name, document.text()) }
    }

    private fun snapshotOf(name: String, text: String): Outcome<StandardSnapshot, LookupFailure> {
        val json = JsonRecords.objectOf(text)
            ?: return Outcome.Failure(LookupFailure.Unavailable("The record of $name is no JSON."))
        val state = json.text("state")
        val rfc = json.text("rfc")?.let { number -> "RFC$number" }
        val snapshot = StandardSnapshot(
            title = json.text("title").orEmpty(),
            status = state,
            revision = json.text("rev"),
            published = json.text("time")?.substringBefore(' '),
            withdrawn = state in CLOSED_STATES,
            replacedBy = listOfNotNull(rfc)
        )
        return Outcome.Success(snapshot)
    }
}
