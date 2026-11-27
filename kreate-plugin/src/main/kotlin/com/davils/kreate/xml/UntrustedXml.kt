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

package com.davils.kreate.xml

import com.davils.kxml.core.Xml
import com.davils.kxml.core.XmlDoctype
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.source.XmlSource
import com.davils.kxml.core.xml

internal object UntrustedXml {
    private val refusing: Xml = xml { doctype = XmlDoctype.Refuse }

    private val internalSubset: Xml = xml { doctype = XmlDoctype.Internal }

    fun rootOf(bytes: ByteArray, isDoctypeAllowed: Boolean = false): XmlNode.Element? {
        val reader = internalSubset.takeIf { isDoctypeAllowed } ?: refusing
        val result = reader.parse(XmlSource.of(bytes))
        return (result as? XmlParseResult.Parsed)?.document?.root
    }
}
