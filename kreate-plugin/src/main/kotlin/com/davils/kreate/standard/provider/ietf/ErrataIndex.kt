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
import com.davils.kore.core.text.SafeText
import com.davils.kreate.standard.fetch.FetchRequest
import com.davils.kreate.standard.fetch.FetchedDocument
import com.davils.kreate.standard.model.ErratumState
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.provider.JsonRecords.text
import com.davils.kreate.standard.provider.ProviderContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeToSequence
import java.io.IOException
import java.io.InputStream

private const val MAX_ERRATA_BYTES: Int = 128 * 1024 * 1024

private val STATES: Map<String, ErratumState> = mapOf(
    "Verified" to ErratumState.VERIFIED,
    "Held for Document Update" to ErratumState.HELD,
    "Reported" to ErratumState.REPORTED,
    "Rejected" to ErratumState.REJECTED
)

internal class ErrataIndex(private val byDocument: Map<String, Map<String, ErratumState>>) {
    fun errataOf(documentId: String): Map<String, ErratumState> = byDocument[documentId].orEmpty()

    companion object {
        fun load(context: ProviderContext): Outcome<ErrataIndex, LookupFailure> {
            val uri = context.endpoints.resolve(context.endpoints.rfcEditor, "api/v1/errata.json")
            val fetched = context.fetch(FetchRequest(uri, MAX_ERRATA_BYTES, isBulk = true))
            return when (fetched) {
                is Outcome.Success -> indexOf(fetched.value)
                is Outcome.Failure -> Outcome.Failure(LookupFailure.Unavailable(fetched.failure.reason))
            }
        }

        private fun indexOf(document: FetchedDocument): Outcome<ErrataIndex, LookupFailure> = try {
            val file = document.path.toFile()
            val entries = file.inputStream().use { stream -> entriesOf(stream) }
            val byDocument = entries.groupBy({ entry -> entry.first }, { entry -> entry.second })
            Outcome.Success(ErrataIndex(byDocument.mapValues { (_, errata) -> errata.toMap() }))
        } catch (cause: SerializationException) {
            unavailable("The errata of the RFC Editor are no JSON", cause)
        } catch (cause: IOException) {
            unavailable("The errata of the RFC Editor could not be read", cause)
        }

        private fun unavailable(reason: String, cause: Exception): Outcome<ErrataIndex, LookupFailure> {
            val detail = SafeText.text(cause.message.orEmpty())
            return Outcome.Failure(LookupFailure.Unavailable("$reason: $detail"))
        }

        @OptIn(ExperimentalSerializationApi::class)
        private fun entriesOf(stream: InputStream): List<Pair<String, Pair<String, ErratumState>>> {
            val elements = Json.decodeToSequence(stream, JsonElement.serializer(), DecodeSequenceMode.ARRAY_WRAPPED)
            return elements.mapNotNull { element -> entryOf(element) }.toList()
        }

        private fun entryOf(element: JsonElement): Pair<String, Pair<String, ErratumState>>? {
            val json = element as? JsonObject ?: return null
            val document = json.text("doc-id") ?: return null
            val id = json.text("errata_id") ?: return null
            val state = STATES[json.text("errata_status_code")] ?: return null
            return document to (id to state)
        }
    }
}
