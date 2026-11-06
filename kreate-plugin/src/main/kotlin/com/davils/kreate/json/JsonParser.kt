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

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

private const val BYTE_ORDER_MARK: Char = '﻿'

private const val FIRST_UNESCAPED_CHARACTER: Char = ' '

private const val UNICODE_ESCAPE_DIGITS: Int = 4

private const val HEX_RADIX: Int = 16

private const val TRUE_LITERAL: String = "true"

private const val FALSE_LITERAL: String = "false"

private const val NULL_LITERAL: String = "null"

private const val UNTERMINATED_STRING: String = "The string is not terminated."

private val NON_FINITE_LITERALS: List<String> = listOf("NaN", "-Infinity", "Infinity")

private val SIMPLE_ESCAPES: Map<Char, Char> = mapOf(
    '"' to '"',
    '\\' to '\\',
    '/' to '/',
    'b' to '\b',
    'f' to '\u000C',
    'n' to '\n',
    'r' to '\r',
    't' to '\t'
)

private val HEX_DIGITS: Set<Char> = buildSet {
    addAll('0'..'9')
    addAll('a'..'f')
    addAll('A'..'F')
}

private val JSON_WHITESPACE: Set<Char> = setOf(' ', '\t', '\n', '\r')

internal class JsonParser private constructor(
    private val text: String,
    private val options: JsonParserOptions
) {
    private var position: Int = 0

    private fun parseDocument(): JsonValue {
        skipWhitespace()
        val value = parseValue(depth = 0)
        skipWhitespace()

        if (position < text.length) fail("The document continues after its value.")
        return value
    }

    private fun parseValue(depth: Int): JsonValue {
        when (text.getOrNull(position)) {
            '{' -> return parseObject(depth + 1)
            '[' -> return parseArray(depth + 1)
            '"' -> return JsonString(parseString())
        }
        return parseScalar()
    }

    private fun parseScalar(): JsonValue {
        when (text.getOrNull(position)) {
            't' -> return parseLiteral(TRUE_LITERAL, JsonBoolean(true))
            'f' -> return parseLiteral(FALSE_LITERAL, JsonBoolean(false))
            'n' -> return parseLiteral(NULL_LITERAL, JsonNull)
        }
        return parseNumber()
    }

    private fun parseObject(depth: Int): JsonObject {
        enterContainer(depth)
        val members = LinkedHashMap<String, JsonValue>()
        if (consumeEmptyContainer('}')) return JsonObject(members)

        do {
            parseMember(members, depth)
        } while (consumeSeparator('}'))
        return JsonObject(members)
    }

    private fun parseMember(members: MutableMap<String, JsonValue>, depth: Int) {
        if (text.getOrNull(position) != '"') fail("An object member needs a string key.")

        val key = parseString()
        skipWhitespace()
        if (text.getOrNull(position) != ':') fail("An object member needs a ':' after its key.")

        position++
        skipWhitespace()
        members[key] = parseValue(depth)
    }

    private fun parseArray(depth: Int): JsonArray {
        enterContainer(depth)
        val elements = ArrayList<JsonValue>()
        if (consumeEmptyContainer(']')) return JsonArray(elements)

        do {
            elements.add(parseValue(depth))
        } while (consumeSeparator(']'))
        return JsonArray(elements)
    }

    private fun enterContainer(depth: Int) {
        if (depth > options.maxDepth) fail("The document nests deeper than ${options.maxDepth} levels.")
        position++
    }

    private fun consumeEmptyContainer(close: Char): Boolean {
        skipWhitespace()
        if (text.getOrNull(position) != close) return false

        position++
        return true
    }

    private fun consumeSeparator(close: Char): Boolean {
        skipWhitespace()
        val separator = text.getOrNull(position)
        if (separator != close && separator != ',') fail("Expected ',' or '$close'.")

        position++
        if (separator == close) return false

        skipWhitespace()
        return true
    }

    private fun parseString(): String {
        position++
        val content = StringBuilder()
        while (true) {
            val character = text.getOrNull(position) ?: fail(UNTERMINATED_STRING)
            position++
            if (character == '"') return content.toString()

            content.append(decodeCharacter(character))
        }
    }

    private fun decodeCharacter(character: Char): Char {
        if (character == '\\') return parseEscape()
        if (character < FIRST_UNESCAPED_CHARACTER) fail("A string holds an unescaped control character.")
        return character
    }

    private fun parseEscape(): Char {
        val escape = text.getOrNull(position) ?: fail(UNTERMINATED_STRING)
        position++
        if (escape == 'u') return parseUnicodeEscape()
        return SIMPLE_ESCAPES[escape] ?: fail("A string holds an invalid escape sequence.")
    }

    private fun parseUnicodeEscape(): Char {
        val end = position + UNICODE_ESCAPE_DIGITS
        if (end > text.length) fail(UNTERMINATED_STRING)

        val digits = text.substring(position, end)
        val isHexadecimal = digits.all { digit -> digit in HEX_DIGITS }
        if (!isHexadecimal) fail("A unicode escape needs four hexadecimal digits.")

        position = end
        return Char(digits.toInt(HEX_RADIX))
    }

    private fun parseLiteral(literal: String, value: JsonValue): JsonValue {
        if (!text.startsWith(literal, position)) fail("Unknown literal.")

        position += literal.length
        return value
    }

    private fun parseNumber(): JsonValue {
        val nonFinite = nonFiniteLiteral()
        if (nonFinite != null) return parseLiteral(nonFinite, JsonNull)

        val end = JsonNumberGrammar.endOf(text, position)
        if (end == JsonNumberGrammar.NOT_A_NUMBER) fail("Expected a JSON value.")

        val literal = text.substring(position, end)
        position = end
        return JsonNumber(literal)
    }

    private fun nonFiniteLiteral(): String? {
        if (!options.nonFiniteAsNull) return null
        return NON_FINITE_LITERALS.firstOrNull { literal -> text.startsWith(literal, position) }
    }

    private fun skipWhitespace() {
        while (position < text.length && text[position] in JSON_WHITESPACE) position++
    }

    private fun fail(message: String): Nothing = throw JsonParseException(message, position)

    companion object {
        fun parse(text: String, options: JsonParserOptions = JsonParserOptions()): JsonValue {
            requireWithinSize(text.length, options)
            val content = text.removePrefix(BYTE_ORDER_MARK.toString())
            val parser = JsonParser(content, options)
            return parser.parseDocument()
        }

        fun parse(bytes: ByteArray, options: JsonParserOptions = JsonParserOptions()): JsonValue {
            requireWithinSize(bytes.size, options)
            return parse(decodeUtf8(bytes), options)
        }

        private fun requireWithinSize(size: Int, options: JsonParserOptions) {
            if (size <= options.maxSize) return
            throw JsonParseException("The document is larger than ${options.maxSize}.", 0)
        }

        private fun decodeUtf8(bytes: ByteArray): String {
            val decoder = Charsets.UTF_8.newDecoder()
            decoder.onMalformedInput(CodingErrorAction.REPORT)
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT)

            return try {
                decoder.decode(ByteBuffer.wrap(bytes)).toString()
            } catch (cause: CharacterCodingException) {
                throw JsonParseException("The document is not valid UTF-8.", 0, cause)
            }
        }
    }
}
