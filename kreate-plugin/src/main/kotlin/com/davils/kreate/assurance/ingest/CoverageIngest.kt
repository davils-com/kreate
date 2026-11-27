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

import com.davils.kore.core.parseDecimalOrNull
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.policy.REPORT_ONLY_ATTRIBUTE
import com.davils.kreate.assurance.rule.ruleFinding
import com.davils.kreate.xml.UntrustedXml
import com.davils.kreate.xml.attributeValue
import com.davils.kreate.xml.children
import java.util.Locale

private const val PERCENT: Double = 100.0

internal object CoverageIngest {
    fun lineCoverageOf(xml: ByteArray): Double? {
        val report = UntrustedXml.rootOf(xml, isDoctypeAllowed = true) ?: return null
        val counters = report.children("counter")
        val lines = counters.firstOrNull { counter -> counter.attributeValue("type") == "LINE" } ?: return null
        val missed = parseDecimalOrNull(lines.attributeValue("missed").orEmpty()) ?: return null
        val covered = parseDecimalOrNull(lines.attributeValue("covered").orEmpty()) ?: return null
        val total = missed + covered
        if (total == 0.0) return null
        return covered / total * PERCENT
    }

    fun findingsOf(coverage: Double?, minimum: Int?): List<Finding> {
        if (coverage == null || minimum == null || coverage >= minimum) return emptyList()
        val measured = String.format(Locale.ROOT, "%.1f", coverage)
        val title = "Line coverage is $measured %, below the minimum of $minimum %"
        val attributes = mapOf(REPORT_ONLY_ATTRIBUTE to "true", "coverage" to measured, "minimum" to minimum.toString())
        return listOf(ruleFinding("COVERAGE_BELOW_MINIMUM", null, title, attributes))
    }
}
