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
package com.davils.kreate.provenance.confusion

import com.davils.kreate.net.HttpMethod
import com.davils.kreate.net.HttpTransport
import com.davils.kreate.net.NetworkException
import com.davils.kreate.net.TransportRequest
import com.davils.kreate.net.TransportResponse
import com.davils.kreate.xml.UntrustedXml
import com.davils.kreate.xml.descendants
import com.davils.kxml.core.node.XmlNode
import java.net.URI

private const val MAX_METADATA_BYTES: Int = 2 * 1024 * 1024

internal class MavenMetadataClient(private val transport: HttpTransport, baseUrl: String) {
    private val base: String = baseUrl.trimEnd('/')

    private val host: Set<String> = setOfNotNull(URI(base).host)

    fun latestVersionOf(group: String, artifact: String): String? {
        val path = "/${group.replace('.', '/')}/$artifact/maven-metadata.xml"
        val uri = URI(base + path)
        val request = TransportRequest(HttpMethod.GET, uri, maxBytes = MAX_METADATA_BYTES, allowedHosts = host)
        val response = send(request)
        if (response.isNotFound) return null
        if (!response.isSuccess) {
            val status = response.statusCode
            throw PublicRepositoryUnavailableException("The public repository answered $path with $status.")
        }
        val document = parse(response.body)
        return newestOf(document) ?: UNKNOWN_VERSION
    }

    private fun send(request: TransportRequest): TransportResponse = try {
        transport.send(request)
    } catch (cause: NetworkException) {
        val reason = cause.message.orEmpty()
        throw PublicRepositoryUnavailableException("The public repository could not be reached: $reason", cause)
    }

    private fun parse(body: ByteArray): XmlNode.Element = UntrustedXml.rootOf(body)
        ?: throw PublicRepositoryUnavailableException("The public repository answered with invalid metadata.")

    private fun newestOf(document: XmlNode.Element): String? {
        val names = listOf("release", "latest", "version")
        return names.firstNotNullOfOrNull { name -> lastTextOf(document, name) }
    }

    private fun lastTextOf(document: XmlNode.Element, name: String): String? {
        val nodes = document.descendants(name)
        val text = nodes.lastOrNull()?.text ?: return null
        return text.trim().ifEmpty { null }
    }

    companion object {
        const val UNKNOWN_VERSION: String = "unknown"
    }
}
