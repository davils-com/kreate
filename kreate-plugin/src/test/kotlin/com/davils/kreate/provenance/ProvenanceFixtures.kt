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
package com.davils.kreate.provenance

import com.davils.kreate.net.ScriptedTransport
import com.davils.kreate.net.TransportRequest
import com.davils.kreate.net.TransportResponse
import com.davils.kreate.net.responseOf

private const val HTTP_OK: Int = 200

private const val HTTP_NOT_FOUND: Int = 404

internal const val PACKAGE_PATH: String = "/v3/systems/maven/packages/org.example%3Alib"

internal const val VERSION_PATH: String = "$PACKAGE_PATH/versions/1.0.0"

internal const val PROJECT_PATH: String = "/v3/projects/github.com%2Fexample%2Flib"

internal fun routedTransport(routes: Map<String, String>): ScriptedTransport =
    ScriptedTransport { request -> answerOf(request, routes) }

private fun answerOf(request: TransportRequest, routes: Map<String, String>): TransportResponse {
    val uri = request.uri.toString()
    val body = routes[request.uri.rawPath] ?: return responseOf(HTTP_NOT_FOUND, uri)
    return responseOf(HTTP_OK, uri, body = body.toByteArray())
}

internal fun historyJson(vararg versions: Pair<String, String>): String {
    val entries = versions.map { (version, published) ->
        """{"versionKey":{"system":"MAVEN","version":"$version"},"publishedAt":"$published"}"""
    }
    return """{"versions":[${entries.joinToString(",")}]}"""
}

internal fun versionJson(project: String): String =
    """{"relatedProjects":[{"projectKey":{"id":"$project"},"relationType":"SOURCE_REPO"}]}"""

internal fun projectJson(score: Double): String = """{"scorecard":{"overallScore":$score}}"""
