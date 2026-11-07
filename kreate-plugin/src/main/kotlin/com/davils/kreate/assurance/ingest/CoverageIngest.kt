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
import com.davils.kreate.assurance.policy.REPORT_ONLY_ATTRIBUTE
import com.davils.kreate.assurance.rule.ruleFinding
import org.w3c.dom.Element
import org.xml.sax.SAXException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.Locale
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

private const val DISALLOW_DOCTYPE: String = "http://apache.org/xml/features/disallow-doctype-decl"

private const val LOAD_EXTERNAL_DTD: String = "http://apache.org/xml/features/nonvalidating/load-external-dtd"

private const val PERCENT: Double = 100.0

internal object CoverageIngest {
    fun lineCoverageOf(xml: ByteArray): Double? {
        val report = parse(xml) ?: return null
        val counters = childrenOf(report).filter { element -> element.tagName == "counter" }
        val lines = counters.firstOrNull { counter -> counter.getAttribute("type") == "LINE" } ?: return null
        val missed = lines.getAttribute("missed").toDoubleOrNull() ?: return null
        val covered = lines.getAttribute("covered").toDoubleOrNull() ?: return null
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

    private fun parse(xml: ByteArray): Element? = try {
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature(LOAD_EXTERNAL_DTD, false)
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        factory.isXIncludeAware = false
        factory.isExpandEntityReferences = false
        factory.newDocumentBuilder().parse(ByteArrayInputStream(xml)).documentElement
    } catch (_: ParserConfigurationException) {
        null
    } catch (_: SAXException) {
        null
    } catch (_: IOException) {
        null
    }

    private fun childrenOf(element: Element): List<Element> {
        val nodes = element.childNodes
        return (0 until nodes.length).mapNotNull { index -> nodes.item(index) as? Element }
    }
}
