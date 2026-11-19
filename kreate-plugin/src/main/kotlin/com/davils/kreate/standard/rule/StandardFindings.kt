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
package com.davils.kreate.standard.rule

import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.SourceLocation
import com.davils.kreate.assurance.rule.RuleCatalog
import com.davils.kreate.standard.model.StandardId

internal object StandardFindings {
    fun findingOf(
        ruleId: String,
        id: StandardId,
        title: String,
        url: String? = null,
        attributes: Map<String, String> = emptyMap(),
        location: SourceLocation? = null
    ): Finding {
        val rule = RuleCatalog.require(ruleId)
        val module = id.toString()
        return Finding(
            ruleId = rule.id,
            source = rule.source,
            severity = rule.severity,
            title = title,
            description = rule.explanation,
            location = location,
            module = module,
            aliases = listOf(module),
            url = url,
            attributes = attributes
        )
    }
}
