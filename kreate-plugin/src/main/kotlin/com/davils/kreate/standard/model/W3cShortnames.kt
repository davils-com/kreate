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
package com.davils.kreate.standard.model

private val DATED_NAME: Regex = Regex("^[a-z]+-(.+)-[0-9]{8}$")

private val ALIASES: Map<String, String> = mapOf(
    "rec-xml" to "xml",
    "rec-xml-names" to "xml-names",
    "xml-c14n" to "xml-c14n10"
)

private val VERSIONS: Map<String, List<String>> = mapOf(
    "xpath" to listOf("xpath-10", "xpath-31"),
    "xml-c14n" to listOf("xml-c14n10", "xml-c14n11")
)

internal object W3cShortnames {
    fun linked(name: String): StandardId {
        val lowered = name.trim().lowercase()
        val dated = DATED_NAME.matchEntire(lowered)
        val undated = dated?.groupValues?.get(1) ?: lowered
        return StandardIds.w3c(ALIASES[undated] ?: undated)
    }

    fun versionsOf(id: StandardId): List<StandardId> {
        if (id.kind != StandardKind.W3C) return emptyList()
        val versions = VERSIONS[id.name].orEmpty()
        return versions.map { version -> StandardId(StandardKind.W3C, version) }
    }
}
