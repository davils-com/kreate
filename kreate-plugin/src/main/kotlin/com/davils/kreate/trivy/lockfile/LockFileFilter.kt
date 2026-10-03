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

package com.davils.kreate.trivy.lockfile

private const val COMMENT_MARKER: String = "#"

private const val CONFIGURATION_SEPARATOR: Char = '='

private const val CONFIGURATION_LIST_SEPARATOR: Char = ','

internal fun retainConfigurations(lines: List<String>, configurations: Set<String>): List<String> =
    lines.filter { line -> isRetained(line, configurations) }

private fun isRetained(line: String, configurations: Set<String>): Boolean {
    val isComment = line.startsWith(COMMENT_MARKER)
    if (isComment) return true

    val hasConfigurations = CONFIGURATION_SEPARATOR in line
    if (!hasConfigurations) return false

    val declaredConfigurations = line.substringAfter(CONFIGURATION_SEPARATOR)
    val lineConfigurations = declaredConfigurations.split(CONFIGURATION_LIST_SEPARATOR)
    return lineConfigurations.any { configuration -> configuration in configurations }
}
