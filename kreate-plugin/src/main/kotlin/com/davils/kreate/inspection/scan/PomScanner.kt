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
package com.davils.kreate.inspection.scan

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.SAXException
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

private const val DISALLOW_DOCTYPE: String = "http://apache.org/xml/features/disallow-doctype-decl"

private const val PROJECT_POM: String = "pom.xml"

internal object PomScanner {
    fun scan(pom: ByteArray): List<Hit> {
        val project = parse(pom)?.documentElement ?: return emptyList()
        return relocationOf(project) + repositoriesOf(project)
    }

    private fun parse(pom: ByteArray): Document? = try {
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature(DISALLOW_DOCTYPE, true)
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        factory.isXIncludeAware = false
        factory.isExpandEntityReferences = false
        factory.newDocumentBuilder().parse(ByteArrayInputStream(pom))
    } catch (_: ParserConfigurationException) {
        null
    } catch (_: SAXException) {
        null
    } catch (_: IOException) {
        null
    }

    private fun relocationOf(project: Element): List<Hit> {
        val management = child(project, "distributionManagement") ?: return emptyList()
        val relocation = child(management, "relocation") ?: return emptyList()
        val target = listOfNotNull(text(relocation, "groupId"), text(relocation, "artifactId")).joinToString(":")
        return listOf(Hit("POM_RELOCATED", PROJECT_POM, target.ifEmpty { null }))
    }

    private fun repositoriesOf(project: Element): List<Hit> {
        val repositories = child(project, "repositories") ?: return emptyList()
        val urls = children(repositories).mapNotNull { repository -> text(repository, "url") }
        return urls.map { url -> Hit("POM_REPOSITORY", PROJECT_POM, url) }
    }

    private fun children(element: Element): List<Element> {
        val nodes = element.childNodes
        return (0 until nodes.length).mapNotNull { index -> nodes.item(index) as? Element }
    }

    private fun child(element: Element, name: String): Element? =
        children(element).firstOrNull { candidate -> candidate.tagName == name }

    private fun text(element: Element, name: String): String? {
        val content = child(element, name)?.textContent ?: return null
        return content.trim().ifEmpty { null }
    }
}
