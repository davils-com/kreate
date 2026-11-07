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

private const val INFLATED_MAJOR: Long = 50

private val CALENDAR_YEARS: LongRange = 1990L..2100L

private val CALENDAR_MONTHS: LongRange = 199_001L..210_012L

private val CALENDAR_DAYS: LongRange = 19_900_101L..21_001_231L

private val LEADING_NUMBER: Regex = Regex("^(\\d{1,18})")

internal object InflatedVersions {
    fun isInflated(version: String): Boolean {
        val digits = LEADING_NUMBER.find(version)?.groupValues?.get(1) ?: return false
        val major = digits.toLong()
        if (major <= INFLATED_MAJOR) return false
        return !isCalendar(major)
    }

    private fun isCalendar(major: Long): Boolean =
        major in CALENDAR_YEARS || major in CALENDAR_MONTHS || major in CALENDAR_DAYS
}
