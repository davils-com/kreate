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

package com.davils.kreate.floor.version

private val NUMERIC_VERSION: Regex = Regex("""^\d+(\.\d+)*(-[0-9A-Za-z.+-]+)?$""")

internal class VersionFloorRule(
    private val group: String,
    val floor: String,
    private val excluded: Set<String>
) {
    fun raises(moduleGroup: String, moduleName: String, version: String?): Boolean {
        if (moduleName in excluded) return false
        val isInGroup = moduleGroup == group || moduleGroup.startsWith("$group.")
        if (!isInGroup) return false
        val requested = version ?: return false
        if (!isNumeric(requested)) return false
        return isBelow(requested)
    }

    private fun isBelow(version: String): Boolean {
        val actual = numbersOf(version)
        val wanted = numbersOf(floor)
        val firstDifference = actual.zip(wanted).firstOrNull { (left, right) -> left != right }
        return firstDifference?.let { (left, right) -> left < right } ?: (actual.size < wanted.size)
    }

    companion object {
        fun isNumeric(version: String): Boolean = NUMERIC_VERSION.matches(version)

        private fun numbersOf(version: String): List<Int> {
            val release = version.substringBefore('-')
            val parts = release.split('.')
            return parts.map { part -> part.toIntOrNull() ?: 0 }
        }
    }
}
