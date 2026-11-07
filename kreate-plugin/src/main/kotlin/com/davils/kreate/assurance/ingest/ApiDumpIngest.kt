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
package com.davils.kreate.assurance.ingest

import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.SourceLocation
import com.davils.kreate.assurance.policy.REPORT_ONLY_ATTRIBUTE
import com.davils.kreate.assurance.rule.ruleFinding

internal object ApiDumpIngest {
    fun findingsOf(expected: String?, actual: String?, dumpPath: String): List<Finding> {
        if (expected == null || actual == null) return emptyList()
        if (normalized(expected) == normalized(actual)) return emptyList()
        val attributes = mapOf(REPORT_ONLY_ATTRIBUTE to "true")
        val finding = ruleFinding("API_DUMP_OUTDATED", null, "$dumpPath no longer matches the code", attributes)
        return listOf(finding.copy(location = SourceLocation(dumpPath)))
    }

    private fun normalized(text: String): String = text.replace("\r\n", "\n").trimEnd()
}
