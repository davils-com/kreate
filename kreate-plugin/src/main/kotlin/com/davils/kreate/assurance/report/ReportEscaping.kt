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
package com.davils.kreate.assurance.report

private val XML_ENTITIES: Map<Char, String> = mapOf(
    '&' to "&amp;",
    '<' to "&lt;",
    '>' to "&gt;",
    '"' to "&quot;",
    '\'' to "&#39;"
)

private val MARKDOWN_SPECIAL: Set<Char> = setOf('|', '`', '*', '_', '[', ']', '#', '\\')

internal object ReportEscaping {
    fun xml(text: String): String {
        val safe = SnippetSanitizer.escape(text)
        return buildString {
            safe.forEach { character -> append(xmlOf(character)) }
        }
    }

    fun markdown(text: String): String {
        val safe = SnippetSanitizer.escape(text)
        return buildString {
            safe.forEach { character -> append(markdownOf(character)) }
        }
    }

    private fun xmlOf(character: Char): String = XML_ENTITIES[character] ?: character.toString()

    private fun markdownOf(character: Char): String {
        if (character in MARKDOWN_SPECIAL) return "\\$character"
        if (character == '\n') return " "
        return xmlOf(character)
    }
}
