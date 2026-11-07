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
package com.davils.kreate.governance.add

import com.davils.kreate.governance.catalog.CatalogNames

private const val WITHOUT_VERSION: Int = 2

private const val WITH_VERSION: Int = 3

internal data class ModuleRequest(val group: String, val name: String, val version: String?) {
    val module: String
        get() = "$group:$name"

    companion object {
        fun parse(notation: String): ModuleRequest {
            val parts = notation.trim().split(':')
            require(parts.size == WITHOUT_VERSION || parts.size == WITH_VERSION) {
                "Name the module as group:artifact or group:artifact:version, not '$notation'."
            }
            require(CatalogNames.isCoordinatePart(parts[0]) && CatalogNames.isCoordinatePart(parts[1])) {
                "'$notation' is no valid module."
            }
            val version = parts.getOrNull(2)
            require(version == null || CatalogNames.isExactVersion(version)) {
                "'$version' is not an exact version. Name one, such as 1.2.3."
            }
            return ModuleRequest(parts[0], parts[1], version)
        }
    }
}
