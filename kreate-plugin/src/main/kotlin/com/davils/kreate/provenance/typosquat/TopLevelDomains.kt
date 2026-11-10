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

private const val COUNTRY_CODE_LENGTH: Int = 2

private val GENERIC_DOMAINS: Set<String> = setOf(
    "ai", "app", "biz", "cloud", "com", "dev", "digital", "edu", "gov", "info", "int", "io", "me", "mil", "name",
    "net", "online", "org", "page", "pro", "site", "software", "space", "systems", "tech", "tools", "xyz"
)

internal fun startsWithTopLevelDomain(group: String): Boolean {
    val first = group.substringBefore('.')
    return first.length == COUNTRY_CODE_LENGTH || first in GENERIC_DOMAINS
}
