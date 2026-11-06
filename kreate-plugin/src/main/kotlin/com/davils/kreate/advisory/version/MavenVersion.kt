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
package com.davils.kreate.advisory.version

private const val NUMERIC_BEFORE_QUALIFIER: Int = 1

private const val GENERAL_AVAILABILITY_RANK: Int = 5

private const val UNKNOWN_QUALIFIER_RANK: Int = 6

private val QUALIFIER_RANKS: Map<String, Int> = mapOf(
    "alpha" to 0,
    "a" to 0,
    "beta" to 1,
    "b" to 1,
    "milestone" to 2,
    "m" to 2,
    "rc" to 3,
    "cr" to 3,
    "snapshot" to 4,
    "" to GENERAL_AVAILABILITY_RANK,
    "ga" to GENERAL_AVAILABILITY_RANK,
    "final" to GENERAL_AVAILABILITY_RANK,
    "release" to GENERAL_AVAILABILITY_RANK
)

private val TOKEN_PATTERN: Regex = Regex("""[0-9]+|[a-zA-Z]+""")

internal class MavenVersion(version: String) : Comparable<MavenVersion> {
    private val tokens: List<String> = tokensOf(version)

    override fun compareTo(other: MavenVersion): Int {
        val length = maxOf(tokens.size, other.tokens.size)
        val differences = (0 until length).asSequence().map { index ->
            compareTokens(tokens.getOrNull(index), other.tokens.getOrNull(index))
        }
        return differences.firstOrNull { difference -> difference != 0 } ?: 0
    }

    private fun compareTokens(first: String?, second: String?): Int {
        val firstNumber = first?.toBigIntegerOrNull()
        val secondNumber = second?.toBigIntegerOrNull()
        if (firstNumber != null && secondNumber != null) return firstNumber.compareTo(secondNumber)
        if (first == null) return compareMissing(second)
        if (second == null) return -compareMissing(first)
        if (firstNumber != null) return NUMERIC_BEFORE_QUALIFIER
        if (secondNumber != null) return -NUMERIC_BEFORE_QUALIFIER

        val ranks = rankOf(first).compareTo(rankOf(second))
        if (ranks != 0) return ranks
        return first.compareTo(second)
    }

    private fun compareMissing(present: String?): Int {
        val token = present.orEmpty()
        val number = token.toBigIntegerOrNull()
        if (number != null) return -number.signum()
        return GENERAL_AVAILABILITY_RANK.compareTo(rankOf(token))
    }

    private fun rankOf(qualifier: String): Int = QUALIFIER_RANKS[qualifier] ?: UNKNOWN_QUALIFIER_RANK

    private companion object {
        private fun tokensOf(version: String): List<String> {
            val matches = TOKEN_PATTERN.findAll(version).toList()
            return matches.map { match -> match.value.lowercase() }
        }
    }
}
