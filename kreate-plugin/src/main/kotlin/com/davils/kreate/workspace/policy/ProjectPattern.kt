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
package com.davils.kreate.workspace.policy

private const val ANY_DEPTH: String = "**"

private const val ONE_SEGMENT: String = "[^:]*"

internal object ProjectPattern {
    fun matches(pattern: String, path: String): Boolean = regexOf(pattern).matches(path)

    private fun regexOf(pattern: String): Regex {
        val parts = pattern.split(ANY_DEPTH)
        val translated = parts.map { part -> segmentOf(part) }
        return Regex(translated.joinToString(".*"))
    }

    private fun segmentOf(part: String): String {
        val literals = part.split('*')
        return literals.joinToString(ONE_SEGMENT) { literal -> Regex.escape(literal) }
    }
}
