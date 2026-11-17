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

package com.davils.kreate.license.header

private const val BYTE_ORDER_MARK: String = "﻿"

private const val WINDOWS_LINE_BREAK: String = "\r\n"

private const val LINE_BREAK: String = "\n"

internal class LicenseHeader(notice: String) {
    private val lines: List<String> = linesOf(notice)

    val isEmpty: Boolean
        get() = lines.all { line -> line.isBlank() }

    fun render(style: CommentStyle): String = when (style) {
        CommentStyle.BLOCK -> lines.joinToString(LINE_BREAK, "/*\n", "\n */\n") { line -> " * $line".trimEnd() }
        CommentStyle.HASH -> lines.joinToString(LINE_BREAK, "#\n", "\n#\n") { line -> "# $line".trimEnd() }
    }

    fun isPresentIn(text: String, style: CommentStyle): Boolean {
        val withoutMark = text.removePrefix(BYTE_ORDER_MARK)
        val normalized = withoutMark.replace(WINDOWS_LINE_BREAK, LINE_BREAK)
        return normalized.startsWith(render(style))
    }

    private companion object {
        fun linesOf(notice: String): List<String> {
            val indented = notice.trimIndent()
            return indented.trim('\n').lines()
        }
    }
}
