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
package com.davils.kreate.standard.provider.github

import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.outcome.map
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardKind
import com.davils.kreate.standard.model.StandardSnapshot
import com.davils.kreate.standard.provider.JsonRecords.text
import com.davils.kreate.standard.provider.ProviderContext
import com.davils.kreate.standard.provider.StandardProvider
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

private const val DRAFT_SUFFIX: String = ".bs"

internal class WhatwgProvider(context: ProviderContext) : StandardProvider {
    private val requests: GitHubRequests = GitHubRequests(context)

    override fun accepts(declaration: StandardDeclaration): Boolean = declaration.id.kind == StandardKind.WHATWG

    override fun snapshotOf(declaration: StandardDeclaration): Outcome<StandardSnapshot, LookupFailure> {
        val name = declaration.id.name
        val drafts = requests.jsonAt("repos/whatwg/$name/contents/review-drafts")
        return drafts.map { listed ->
            val entries = (listed as? JsonArray).orEmpty().filterIsInstance<JsonObject>()
            val names = entries.mapNotNull { entry -> entry.text("name") }
            val drafts = names.filter { file -> file.endsWith(DRAFT_SUFFIX) }
            val latest = drafts.maxOrNull()?.removeSuffix(DRAFT_SUFFIX)
            StandardSnapshot(title = "WHATWG ${name.uppercase()} Living Standard", revision = latest)
        }
    }
}
