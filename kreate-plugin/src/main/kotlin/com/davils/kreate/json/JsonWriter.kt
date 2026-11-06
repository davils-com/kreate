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

package com.davils.kreate.json

private const val INDENTATION: String = "  "

private const val COMPACT_MEMBER_SEPARATOR: String = ":"

private const val PRETTY_MEMBER_SEPARATOR: String = ": "

internal object JsonWriter {
    fun compact(value: JsonValue): String = render(value, level = null)

    fun pretty(value: JsonValue): String = render(value, level = 0) + "\n"

    fun quote(text: String): String = JsonStringEscaper.quote(text)

    private fun render(value: JsonValue, level: Int?): String = buildString { appendValue(value, level) }

    private fun StringBuilder.appendValue(value: JsonValue, level: Int?) {
        when (value) {
            JsonNull -> append("null")
            is JsonBoolean -> append(value.value)
            is JsonNumber -> append(value.literal)
            is JsonString -> append(quote(value.value))
            is JsonArray -> appendArray(value, level)
            is JsonObject -> appendObject(value, level)
        }
    }

    private fun StringBuilder.appendArray(array: JsonArray, level: Int?) {
        appendContainer('[', ']', array.elements, level) { element, inner -> appendValue(element, inner) }
    }

    private fun StringBuilder.appendObject(json: JsonObject, level: Int?) {
        val members = json.members.entries.sortedBy { member -> member.key }
        appendContainer('{', '}', members, level) { member, inner ->
            append(quote(member.key))
            append(inner?.let { PRETTY_MEMBER_SEPARATOR } ?: COMPACT_MEMBER_SEPARATOR)
            appendValue(member.value, inner)
        }
    }

    private fun <T> StringBuilder.appendContainer(
        open: Char,
        close: Char,
        items: List<T>,
        level: Int?,
        appendItem: StringBuilder.(T, Int?) -> Unit
    ) {
        append(open)
        if (items.isEmpty()) {
            append(close)
            return
        }

        val inner = level?.plus(1)
        items.forEachIndexed { index, item ->
            if (index > 0) append(',')
            appendLineBreak(inner)
            appendItem(item, inner)
        }
        appendLineBreak(level)
        append(close)
    }

    private fun StringBuilder.appendLineBreak(level: Int?) {
        if (level == null) return

        append('\n')
        repeat(level) { append(INDENTATION) }
    }
}
