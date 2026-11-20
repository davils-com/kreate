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

import java.io.Serializable

internal data class StandardDeclaration(
    val id: StandardId,
    val note: String? = null,
    val reviewEverySeconds: Long? = null,
    val tags: String? = null,
    val url: String? = null,
    val errata: String? = null,
    val suite: SuiteSource? = null
) : Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}
