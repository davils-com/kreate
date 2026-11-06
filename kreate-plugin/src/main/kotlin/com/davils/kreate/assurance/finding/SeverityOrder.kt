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
package com.davils.kreate.assurance.finding

import com.davils.kreate.assurance.Severity

private val SEVERITY_NAMES: Map<String, Severity> = mapOf(
    "CRITICAL" to Severity.CRITICAL,
    "HIGH" to Severity.HIGH,
    "MEDIUM" to Severity.MEDIUM,
    "MODERATE" to Severity.MEDIUM,
    "LOW" to Severity.LOW,
    "NONE" to Severity.LOW
)

internal fun Severity.isAtLeast(threshold: Severity): Boolean = ordinal <= threshold.ordinal

internal fun parseSeverity(name: String): Severity {
    val normalized = name.uppercase()
    return SEVERITY_NAMES[normalized] ?: Severity.UNKNOWN
}
