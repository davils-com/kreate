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
package com.davils.kreate.standard.inventory

import com.davils.kore.core.parseIntOrNull
import com.davils.kreate.standard.model.MAX_RFC_NUMBER
import com.davils.kreate.standard.model.StandardId
import com.davils.kreate.standard.model.StandardIds

private val RFC_TEXT: Regex = Regex("""\bRFC[ -]?(\d{1,5})\b""")

private val RFC_LINK: Regex = Regex(
    """(?:rfc-editor\.org/(?:rfc|info)/rfc|datatracker\.ietf\.org/doc/(?:html/)?rfc|""" +
        """tools\.ietf\.org/html/rfc|ietf\.org/rfc/rfc)(\d{1,5})\b"""
)

private val DRAFT_NAME: Regex = Regex("""\b(draft-[a-z0-9]+-[a-z0-9]+(?:-[a-z0-9]+)*)\b""")

private val DRAFT_REVISION: Regex = Regex("-[0-9]{2}$")

private val ISO_TEXT: Regex = Regex("""(?<![A-Z]{2,4} )\bISO(?:/(?:IEC|IEEE|TS|TR|PAS))*[ -](\d{3,5}(?:-\d{1,3})*)\b""")

private const val CHARSET_PREFIX: String = "ISO-8859"

private val W3C_LINK: Regex = Regex("""\bw3\.org/TR/([A-Za-z][A-Za-z0-9.-]*[A-Za-z0-9])""")

private val WHATWG_LINK: Regex = Regex("""\b([a-z]+)\.spec\.whatwg\.org\b""")

private val UNICODE_TEXT: Regex = Regex("""\b(UAX|UTS) ?#?(\d{1,3})\b""")

private val DIN_TEXT: Regex = Regex("""\bDIN (?:(?:EN|ISO|IEC|SPEC|VDE) )*\d{3,5}(?:-\d{1,3})*\b""")

internal object ReferencePatterns {
    fun idsIn(line: String): List<StandardId> {
        val rfcs = (RFC_TEXT.findAll(line) + RFC_LINK.findAll(line)).mapNotNull { match -> rfcOf(match) }
        val drafts = DRAFT_NAME.findAll(line).mapNotNull { match -> draftOf(match.groupValues[1]) }
        val isos = ISO_TEXT.findAll(line).mapNotNull { match -> isoOf(match.value) }
        val w3c = W3C_LINK.findAll(line).mapNotNull { match -> idOrNull { StandardIds.w3c(match.groupValues[1]) } }
        val whatwg = WHATWG_LINK.findAll(line).mapNotNull { match -> whatwgOf(match.groupValues[1]) }
        val unicode = UNICODE_TEXT.findAll(line).mapNotNull { match -> idOrNull { StandardIds.unicode(match.value) } }
        val din = DIN_TEXT.findAll(line).mapNotNull { match -> idOrNull { StandardIds.din(match.value) } }
        val found = rfcs + drafts + isos + w3c + whatwg + unicode + din
        return found.distinct().toList()
    }

    private fun rfcOf(match: MatchResult): StandardId? {
        val number = parseIntOrNull(match.groupValues[1]) ?: return null
        if (number !in 1..MAX_RFC_NUMBER) return null
        return StandardIds.rfc(number)
    }

    private fun isoOf(reference: String): StandardId? {
        if (reference.startsWith(CHARSET_PREFIX)) return null
        return idOrNull { StandardIds.iso(reference) }
    }

    private fun whatwgOf(name: String): StandardId? = idOrNull { StandardIds.whatwg(name) }

    private fun draftOf(name: String): StandardId? {
        val withoutRevision = name.replace(DRAFT_REVISION, "")
        return idOrNull { StandardIds.draft(withoutRevision) }
    }

    private fun idOrNull(factory: () -> StandardId): StandardId? = try {
        factory()
    } catch (_: IllegalArgumentException) {
        null
    }
}
