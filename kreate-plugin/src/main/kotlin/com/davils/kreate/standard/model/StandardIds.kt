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

import com.davils.kore.core.text.SafeText

private const val ISO_PREFIX: String = "iso"

private const val DIN_PREFIX: String = "din"

private val ISO_YEAR: Regex = Regex(":[0-9]{4}$")

private val DRAFT_REVISION: Regex = Regex("-[0-9]{2}$")

private val UNICODE_INPUT: Regex = Regex("^(uax|uts)\\s*#?\\s*([1-9][0-9]{0,2})$")

private val SEPARATOR_RUNS: Regex = Regex("[\\s/]+")

private val SLUG_SEPARATORS: Regex = Regex("[^a-z0-9.]+")

internal object StandardIds {
    fun rfc(number: Int): StandardId {
        require(number in 1..MAX_RFC_NUMBER) {
            "An RFC number lies between 1 and $MAX_RFC_NUMBER, but was $number."
        }
        return StandardId(StandardKind.RFC, number.toString())
    }

    fun draft(name: String): StandardId {
        val normalized = name.trim().lowercase()
        require(!DRAFT_REVISION.containsMatchIn(normalized)) {
            "${SafeText.quoted(name)} names a revision of a draft; declare the draft without it."
        }
        return idOf(StandardKind.DRAFT, normalized, name)
    }

    fun iso(reference: String): StandardId {
        val lowered = reference.trim().lowercase()
        val withoutYear = lowered.replace(ISO_YEAR, "")
        val withoutPrefix = withoutYear.removePrefix(ISO_PREFIX)
        val separated = withoutPrefix.replace(SEPARATOR_RUNS, "-")
        return idOf(StandardKind.ISO, separated.trim('-'), reference)
    }

    fun w3c(shortname: String): StandardId = idOf(StandardKind.W3C, shortname.trim().lowercase(), shortname)

    fun suite(name: String): StandardId = idOf(StandardKind.SUITE, name.trim().lowercase(), name)

    fun whatwg(name: String): StandardId = idOf(StandardKind.WHATWG, name.trim().lowercase(), name)

    fun unicode(report: String?): StandardId {
        val text = report ?: return StandardId(StandardKind.UNICODE, UNICODE_DATABASE)
        val lowered = text.trim().lowercase()
        val match = UNICODE_INPUT.matchEntire(lowered)
        val name = match?.let { found -> "${found.groupValues[1]}-${found.groupValues[2]}" } ?: lowered
        return idOf(StandardKind.UNICODE, name, text)
    }

    fun github(repository: String): StandardId =
        idOf(StandardKind.GITHUB, repository.trim().lowercase(), repository)

    fun din(number: String): StandardId {
        val lowered = number.trim().lowercase()
        val withoutPrefix = lowered.removePrefix(DIN_PREFIX)
        return idOf(StandardKind.DIN, slugOf(withoutPrefix), number)
    }

    fun document(name: String): StandardId = idOf(StandardKind.DOCUMENT, slugOf(name.lowercase()), name)

    private fun idOf(kind: StandardKind, name: String, input: String): StandardId {
        require(StandardIdGrammar.isValid(kind, name)) {
            "${SafeText.quoted(input)} is no ${kind.id} standard such as ${SafeText.quoted(kind.example)}."
        }
        return StandardId(kind, name)
    }
}

private fun slugOf(text: String): String {
    val separated = text.replace(SLUG_SEPARATORS, "-")
    return separated.trim('-', '.')
}
