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

import com.davils.kreate.xml.UntrustedXml
import com.davils.kreate.xml.child
import com.davils.kreate.xml.childText
import com.davils.kxml.core.node.XmlNode

private const val PROJECT_POM: String = "pom.xml"

internal object PomScanner {
    fun scan(pom: ByteArray): List<Hit> {
        val project = UntrustedXml.rootOf(pom) ?: return emptyList()
        return relocationOf(project) + repositoriesOf(project)
    }

    private fun relocationOf(project: XmlNode.Element): List<Hit> {
        val management = project.child("distributionManagement") ?: return emptyList()
        val relocation = management.child("relocation") ?: return emptyList()
        val parts = listOfNotNull(relocation.childText("groupId"), relocation.childText("artifactId"))
        val target = parts.joinToString(":")
        return listOf(Hit("POM_RELOCATED", PROJECT_POM, target.ifEmpty { null }))
    }

    private fun repositoriesOf(project: XmlNode.Element): List<Hit> {
        val repositories = project.child("repositories") ?: return emptyList()
        val urls = repositories.elements.mapNotNull { repository -> repository.childText("url") }
        return urls.map { url -> Hit("POM_REPOSITORY", PROJECT_POM, url) }
    }
}
