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

internal enum class StandardKind(val id: String, val example: String, val isMachineChecked: Boolean) {
    RFC("rfc", "9562", isMachineChecked = true),
    DRAFT("draft", "draft-ietf-httpbis-resumable-upload", isMachineChecked = true),
    ISO("iso", "ISO 8601-1", isMachineChecked = true),
    W3C("w3c", "webauthn-3", isMachineChecked = true),
    WHATWG("whatwg", "url", isMachineChecked = true),
    UNICODE("unicode", "UAX #31", isMachineChecked = true),
    GITHUB("github", "yaml/yaml-spec", isMachineChecked = true),
    DIN("din", "DIN 5008", isMachineChecked = false),
    DOCUMENT("document", "ECMA-404", isMachineChecked = false);

    companion object {
        fun parse(id: String): StandardKind? = entries.firstOrNull { kind -> kind.id == id }
    }
}
