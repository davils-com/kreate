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
package com.davils.kreate.standard.provider.document

import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.outcome.map
import com.davils.kreate.standard.fetch.FetchRequest
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardKind
import com.davils.kreate.standard.model.StandardSnapshot
import com.davils.kreate.standard.provider.ProviderContext
import com.davils.kreate.standard.provider.StandardProvider
import java.net.URI

private val WATCHED_KINDS: Set<StandardKind> = setOf(StandardKind.DIN, StandardKind.DOCUMENT)

internal class DocumentProvider(private val context: ProviderContext) : StandardProvider {
    override fun accepts(declaration: StandardDeclaration): Boolean =
        declaration.id.kind in WATCHED_KINDS && declaration.url != null

    override fun snapshotOf(declaration: StandardDeclaration): Outcome<StandardSnapshot, LookupFailure> {
        val url = URI(declaration.url.orEmpty())
        val fetched = context.fetch(FetchRequest(url))
        return fetched.map { document ->
            val fingerprint = Fingerprints.fingerprintOf(document.bytes())
            StandardSnapshot(title = declaration.id.toString(), fingerprint = fingerprint)
        }
    }
}
