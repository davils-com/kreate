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
package com.davils.kreate.provenance.typosquat

private val HOSTING_PREFIXES: Set<String> = setOf("io.github", "com.github", "io.gitlab", "com.gitlab", "org.bitbucket")

private const val OWNER_LABELS: Int = 2

internal fun ownerOf(group: String): String {
    val labels = group.split('.')
    val prefix = labels.take(OWNER_LABELS).joinToString(".")
    val owned = OWNER_LABELS + hostingExtra(prefix)
    return labels.take(owned).joinToString(".")
}

internal fun sharedLabels(first: String, second: String): Int {
    val pairs = first.split('.').zip(second.split('.'))
    return pairs.takeWhile { (one, other) -> one == other }.size
}

internal fun requiredSharedLabels(pluginId: String): Int {
    val labels = pluginId.split('.')
    val prefix = labels.take(OWNER_LABELS).joinToString(".")
    return OWNER_LABELS + hostingExtra(prefix)
}

private fun hostingExtra(prefix: String): Int {
    if (prefix in HOSTING_PREFIXES) return 1
    return 0
}
