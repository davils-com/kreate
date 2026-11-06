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
package com.davils.kreate.assurance.baseline

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.finding.Finding

internal data class BaselineEntry(
    val source: String,
    val rule: String,
    val count: Int,
    val severity: Severity,
    val title: String,
    val module: String? = null,
    val path: String? = null
) {
    val key: String
        get() = baselineKey(source, rule, module, path)

    companion object {
        fun keyOf(finding: Finding): String =
            baselineKey(finding.source.id, finding.ruleId, finding.module, finding.location?.path)

        private fun baselineKey(source: String, rule: String, module: String?, path: String?): String =
            listOf(source, rule, module.orEmpty(), path.orEmpty()).joinToString("|")
    }
}
