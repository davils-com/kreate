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
package com.davils.kreate.assurance.policy

import com.davils.kreate.assurance.finding.Finding
import java.time.LocalDate

internal class IgnoreRule(
    val id: String,
    val reason: String,
    val module: String? = null,
    val expires: LocalDate? = null
) {
    fun isExpired(today: LocalDate): Boolean = expires != null && today.isAfter(expires)

    fun matches(finding: Finding): Boolean {
        val matchesModule = module == null || module == finding.module
        return matchesModule && id in finding.identifiers
    }
}
