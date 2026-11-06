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
package com.davils.kreate.advisory.osv

import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.net.HttpMethod
import com.davils.kreate.net.HttpTransport
import com.davils.kreate.net.NetworkException
import com.davils.kreate.net.TransportRequest
import com.davils.kreate.net.TransportResponse
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URI

private const val BATCH_SIZE: Int = 500

private const val MAX_PAGES: Int = 20

private const val ECOSYSTEM: String = "Maven"

private const val CONTENT_TYPE_HEADER: String = "Content-Type"

private const val JSON_CONTENT_TYPE: String = "application/json"

internal class OsvClient(
    private val transport: HttpTransport,
    baseUrl: String,
    private val cache: OsvCache
) {
    private val base: String = baseUrl.trimEnd('/')

    private val host: Set<String> = setOfNotNull(URI(base).host)

    fun recordsOf(modules: List<ModuleCoordinate>): Map<ModuleCoordinate, List<OsvRecord>> {
        val batches = modules.chunked(BATCH_SIZE)
        val listed = batches.flatMap { batch -> listBatch(batch) }
        return listed.associate { (module, references) -> module to references.map(::recordOf) }
    }

    private fun listBatch(batch: List<ModuleCoordinate>): List<Pair<ModuleCoordinate, List<OsvReference>>> =
        batch.zip(firstPages(batch)).map { (module, page) -> module to allPagesOf(module, page) }

    private fun firstPages(batch: List<ModuleCoordinate>): List<OsvPage> {
        val queries = buildJsonArray { batch.forEach { module -> add(queryOf(module, null)) } }
        val pages = pagesOf(post(buildJsonObject { put("queries", queries) }))
        if (pages.size != batch.size) throw OsvUnavailableException("OSV answered a different number of queries.")
        return pages
    }

    private fun allPagesOf(module: ModuleCoordinate, first: OsvPage): List<OsvReference> {
        val references = first.references.toMutableList()
        var token = first.nextPageToken
        var page = 1
        while (token != null) {
            if (page >= MAX_PAGES) throw OsvUnavailableException("OSV lists too many pages for ${module.module}.")
            val queries = buildJsonArray { add(queryOf(module, token)) }
            val next = pagesOf(post(buildJsonObject { put("queries", queries) })).single()
            references += next.references
            token = next.nextPageToken
            page += 1
        }
        return references
    }

    private fun queryOf(module: ModuleCoordinate, pageToken: String?): JsonObject = buildJsonObject {
        put(
            "package",
            buildJsonObject {
                put("ecosystem", ECOSYSTEM)
                put("name", module.module)
            }
        )
        put("version", module.version)
        pageToken?.let { token -> put("page_token", token) }
    }

    private fun post(body: JsonObject): JsonElement {
        val request = TransportRequest(
            method = HttpMethod.POST,
            uri = URI("$base/v1/querybatch"),
            headers = mapOf(CONTENT_TYPE_HEADER to JSON_CONTENT_TYPE),
            body = body.toString().toByteArray(),
            allowedHosts = host
        )
        return parse(send(request))
    }

    private fun send(request: TransportRequest): TransportResponse {
        val response = try {
            transport.send(request)
        } catch (cause: NetworkException) {
            throw OsvUnavailableException("OSV could not be reached: ${cause.message}", cause)
        }
        if (!response.isSuccess) {
            throw OsvUnavailableException("OSV answered ${request.uri.path} with status ${response.statusCode}.")
        }
        return response
    }

    private fun parse(response: TransportResponse): JsonElement = try {
        Json.parseToJsonElement(response.text)
    } catch (cause: SerializationException) {
        throw OsvUnavailableException("OSV answered with invalid JSON.", cause)
    }

    private fun pagesOf(answer: JsonElement): List<OsvPage> {
        val results = (answer as? JsonObject)?.get("results") as? JsonArray
            ?: throw OsvUnavailableException("The answer of OSV holds no results.")
        return results.map { result -> pageOf(result as? JsonObject ?: JsonObject(emptyMap())) }
    }

    private fun pageOf(result: JsonObject): OsvPage {
        val entries = result["vulns"] as? JsonArray ?: JsonArray(emptyList())
        val references = entries.filterIsInstance<JsonObject>().mapNotNull(::referenceOf)
        val token = (result["next_page_token"] as? JsonPrimitive)?.content
        return OsvPage(references, token?.ifEmpty { null })
    }

    private fun referenceOf(entry: JsonObject): OsvReference? {
        val id = (entry["id"] as? JsonPrimitive)?.content ?: return null
        return OsvReference(id, (entry["modified"] as? JsonPrimitive)?.content)
    }

    private fun recordOf(reference: OsvReference): OsvRecord {
        val cached = cache.read(reference.id)
        if (cached != null) {
            val record = OsvRecordParser.parse(cached, reference.id)
            if (record.modified != null && record.modified == reference.modified) return record
        }

        val request = TransportRequest(HttpMethod.GET, URI("$base/v1/vulns/${reference.id}"), allowedHosts = host)
        val text = send(request).text
        val record = OsvRecordParser.parse(text, reference.id)
        cache.write(reference.id, text)
        return record
    }
}
