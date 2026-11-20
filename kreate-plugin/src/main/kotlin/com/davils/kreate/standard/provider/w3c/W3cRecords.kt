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

import com.davils.kreate.standard.model.StandardSnapshot
import com.davils.kreate.standard.provider.JsonRecords.child
import com.davils.kreate.standard.provider.JsonRecords.text
import kotlinx.serialization.json.JsonObject
import java.net.URI

private const val LINKS: String = "_links"

private const val HREF: String = "href"

private const val DATE_LENGTH: Int = 8

private const val YEAR_END: Int = 4

private const val MONTH_END: Int = 6

private const val DAY_LENGTH: Int = 2

private val CLOSED_STATUS: Regex = Regex("Superseded|Obsolete|Rescinded|Retired|Discontinued", RegexOption.IGNORE_CASE)

internal object W3cRecords {
    fun snapshotOf(specification: JsonObject): StandardSnapshot {
        val latest = linkOf(specification, "latest-version")
        val status = latest?.text("title")
        val version = latest?.text(HREF)?.let(::lastSegmentOf)
        return StandardSnapshot(
            title = specification.text("title").orEmpty(),
            status = status,
            revision = version?.let(::dateOf),
            published = version?.let(::dateOf),
            withdrawn = status != null && CLOSED_STATUS.containsMatchIn(status)
        )
    }

    fun latestVersionOf(specification: JsonObject): String? {
        val href = linkOf(specification, "latest-version")?.text(HREF) ?: return null
        return URI(href).path
    }

    fun errataOf(version: JsonObject): String? = version.text("errata")

    fun seriesOf(specification: JsonObject): String? {
        val href = linkOf(specification, "series")?.text(HREF) ?: return null
        return URI(href).path
    }

    fun currentOf(series: JsonObject): String? {
        val href = linkOf(series, "current-specification")?.text(HREF) ?: return null
        return lastSegmentOf(href)
    }

    private fun linkOf(record: JsonObject, name: String): JsonObject? {
        val links = record.child(LINKS) ?: return null
        return links.child(name)
    }

    private fun lastSegmentOf(href: String): String {
        val trimmed = href.trimEnd('/')
        return trimmed.substringAfterLast('/')
    }

    private fun dateOf(version: String): String {
        if (version.length != DATE_LENGTH || !version.all(Char::isDigit)) return version
        return "${version.take(YEAR_END)}-${version.substring(YEAR_END, MONTH_END)}-${version.takeLast(DAY_LENGTH)}"
    }
}
