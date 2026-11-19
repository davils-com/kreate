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
package com.davils.kreate.standard.provider.unicode

private val DATABASE_VERSION: Regex =
    Regex("""version (\d{1,3}\.\d{1,3}\.\d{1,3}) of the\s+Unicode Character Database""")

private val TITLE: Regex = Regex("""<title>([^<]{1,200})</title>""", RegexOption.IGNORE_CASE)

private val DATE: Regex = Regex("""<td>Date</td>\s*<td>(\d{4}-\d{2}-\d{2})</td>""")

private const val THIS_VERSION: String = "This Version"

internal object UnicodePages {
    fun databaseVersionOf(readMe: String): String? {
        val match = DATABASE_VERSION.find(readMe) ?: return null
        return match.groupValues[1]
    }

    fun revisionOf(page: String, report: Int): String? {
        val start = page.indexOf(THIS_VERSION)
        if (start < 0) return null
        val revision = Regex("""tr$report-(\d{1,4})\.html""")
        val match = revision.find(page, start) ?: return null
        return match.groupValues[1]
    }

    fun titleOf(page: String): String? {
        val match = TITLE.find(page) ?: return null
        return match.groupValues[1].trim()
    }

    fun dateOf(page: String): String? {
        val match = DATE.find(page) ?: return null
        return match.groupValues[1]
    }
}
