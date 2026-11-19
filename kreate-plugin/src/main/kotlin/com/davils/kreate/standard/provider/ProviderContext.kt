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
package com.davils.kreate.standard.provider

import com.davils.kore.core.outcome.Outcome
import com.davils.kreate.net.HttpTransport
import com.davils.kreate.standard.fetch.FetchRequest
import com.davils.kreate.standard.fetch.FetchedDocument
import com.davils.kreate.standard.fetch.RevalidatingCache
import com.davils.kreate.standard.model.LookupFailure

internal class ProviderContext(
    val endpoints: ServiceEndpoints,
    private val cache: RevalidatingCache,
    private val transport: () -> HttpTransport,
    private val bulkTransport: () -> HttpTransport,
    val githubToken: String? = null
) {
    fun fetch(request: FetchRequest): Outcome<FetchedDocument, LookupFailure> {
        if (request.isBulk) return cache.fetch(request, bulkTransport())
        return cache.fetch(request, transport())
    }
}
