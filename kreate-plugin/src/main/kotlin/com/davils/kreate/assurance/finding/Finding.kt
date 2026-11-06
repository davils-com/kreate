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
package com.davils.kreate.assurance.finding

import com.davils.kreate.assurance.Severity
import java.security.MessageDigest
import java.util.HexFormat

private const val FINGERPRINT_ALGORITHM: String = "SHA-256"

private val UNRATED_MEANS_SERIOUS: Set<FindingSource> = setOf(FindingSource.OSV, FindingSource.TRIVY)

internal data class Finding(
    val ruleId: String,
    val source: FindingSource,
    val severity: Severity,
    val title: String,
    val description: String = "",
    val location: SourceLocation? = null,
    val module: String? = null,
    val version: String? = null,
    val fixedVersion: String? = null,
    val aliases: List<String> = emptyList(),
    val url: String? = null,
    val snippet: String? = null,
    val attributes: Map<String, String> = emptyMap()
) {
    val identifiers: Set<String>
        get() = setOf(ruleId) + aliases

    val gatingSeverity: Severity
        get() {
            val isUnratedAdvisory = severity == Severity.UNKNOWN && source in UNRATED_MEANS_SERIOUS
            if (isUnratedAdvisory) return Severity.HIGH
            return severity
        }

    val fingerprint: String
        get() {
            val identity = listOf(
                source.id,
                ruleId,
                module.orEmpty(),
                version.orEmpty(),
                location?.path.orEmpty(),
                location?.line?.toString().orEmpty()
            )
            val digest = MessageDigest.getInstance(FINGERPRINT_ALGORITHM)
            val hash = digest.digest(identity.joinToString("|").toByteArray())
            return HexFormat.of().formatHex(hash)
        }
}

internal fun Finding.withAttribute(name: String, value: String): Finding =
    copy(attributes = attributes + (name to value))
