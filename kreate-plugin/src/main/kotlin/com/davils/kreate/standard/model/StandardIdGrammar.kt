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

import com.davils.kore.core.naming.NameRule
import com.davils.kore.core.naming.NameStart
import com.davils.kore.core.naming.nameRule
import com.davils.kore.core.parseIntOrNull

internal const val MAX_RFC_NUMBER: Int = 99_999

internal const val UNICODE_DATABASE: String = "ucd"

private const val MAX_NAME_LENGTH: Int = 100

private const val KIND_SEPARATOR: Char = ':'

private const val REPOSITORY_SEPARATOR: Char = '/'

private const val PART_SEPARATOR: Char = '-'

private const val DRAFT_PREFIX: String = "draft-"

private val ISO_NAME: Regex = Regex("^(?:[a-z]+-)*[0-9]+(?:-[0-9]+)*$")

private val UNICODE_REPORT: Regex = Regex("^(?:uax|uts)-[1-9][0-9]{0,2}$")

private val KEBAB_NAME: NameRule = nameRule {
    isStrict = true
    maxLength = MAX_NAME_LENGTH
    example = "resumable-upload"
}

private val DOTTED_NAME: NameRule = nameRule {
    start = NameStart.LetterOrDigit
    separators = "-."
    isStrict = true
    maxLength = MAX_NAME_LENGTH
    example = "webauthn-3"
}

private val REPOSITORY_PART: NameRule = nameRule {
    start = NameStart.LetterOrDigit
    separators = "-_."
    maxLength = MAX_NAME_LENGTH
    example = "yaml-spec"
}

internal object StandardIdGrammar {
    fun parse(text: String): StandardId? {
        val kindId = text.substringBefore(KIND_SEPARATOR, missingDelimiterValue = "")
        val kind = StandardKind.parse(kindId) ?: return null
        val name = text.substringAfter(KIND_SEPARATOR)
        if (!isValid(kind, name)) return null
        return StandardId(kind, name)
    }

    fun isValid(kind: StandardKind, name: String): Boolean = when (kind) {
        StandardKind.RFC -> isRfcNumber(name)
        StandardKind.DRAFT -> name.startsWith(DRAFT_PREFIX) && KEBAB_NAME.matches(name)
        StandardKind.ISO -> name.length <= MAX_NAME_LENGTH && ISO_NAME.matches(name)
        StandardKind.W3C -> DOTTED_NAME.matches(name)
        StandardKind.WHATWG -> KEBAB_NAME.matches(name)
        StandardKind.UNICODE -> name == UNICODE_DATABASE || UNICODE_REPORT.matches(name)
        StandardKind.GITHUB -> isRepository(name)
        StandardKind.DIN, StandardKind.DOCUMENT -> DOTTED_NAME.matches(name)
    }

    fun isoReferenceOf(id: StandardId): String {
        val parts = id.name.split(PART_SEPARATOR)
        val bodies = parts.takeWhile { part -> part.first().isLetter() }
        val number = parts.drop(bodies.size).joinToString(PART_SEPARATOR.toString())
        val joint = bodies.joinToString("") { body -> "/" + body.uppercase() }
        return "ISO$joint $number"
    }

    private fun isRfcNumber(name: String): Boolean {
        val number = parseIntOrNull(name, allowLeadingZeros = false) ?: return false
        return number in 1..MAX_RFC_NUMBER
    }

    private fun isRepository(name: String): Boolean {
        val parts = name.split(REPOSITORY_SEPARATOR)
        if (parts.size != 2) return false
        return parts.all { part -> REPOSITORY_PART.matches(part) }
    }
}
