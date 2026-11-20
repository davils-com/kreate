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
package com.davils.kreate.standard.model

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal object StandardLinks {
    fun infoUrlOf(id: StandardId, declaredUrl: String? = null): String? = when (id.kind) {
        StandardKind.RFC -> "https://www.rfc-editor.org/info/rfc${id.name}"
        StandardKind.DRAFT -> "https://datatracker.ietf.org/doc/${id.name}/"
        StandardKind.ISO -> "https://www.iso.org/search.html?q=${encoded(StandardIdGrammar.isoReferenceOf(id))}"
        StandardKind.W3C -> "https://www.w3.org/TR/${id.name}/"
        StandardKind.WHATWG -> "https://${id.name}.spec.whatwg.org/"
        StandardKind.UNICODE -> unicodeUrlOf(id)
        StandardKind.GITHUB -> "https://github.com/${id.name}"
        StandardKind.DIN, StandardKind.DOCUMENT, StandardKind.SUITE -> declaredUrl
    }

    fun infoUrlOf(declaration: StandardDeclaration): String? {
        val suite = declaration.suite ?: return infoUrlOf(declaration.id, declaration.url)
        val repository = suite.github?.let { name -> "https://github.com/$name" }
        return suite.watch ?: repository ?: suite.archive
    }

    fun erratumUrlOf(declaration: StandardDeclaration): String? {
        val id = declaration.id
        if (id.kind == StandardKind.W3C) return declaration.errata ?: infoUrlOf(id)
        if (id.kind != StandardKind.RFC) return null
        return "https://www.rfc-editor.org/errata/rfc${id.name}"
    }

    private fun unicodeUrlOf(id: StandardId): String {
        if (id.name == "ucd") return "https://www.unicode.org/versions/latest/"
        val report = id.name.substringAfter('-')
        return "https://www.unicode.org/reports/tr$report/"
    }

    private fun encoded(text: String): String = URLEncoder.encode(text, StandardCharsets.UTF_8)
}
