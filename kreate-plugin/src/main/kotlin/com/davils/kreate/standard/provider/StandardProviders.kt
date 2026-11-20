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

import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.provider.document.DocumentProvider
import com.davils.kreate.standard.provider.github.GitHubProvider
import com.davils.kreate.standard.provider.github.WhatwgProvider
import com.davils.kreate.standard.provider.ietf.DraftProvider
import com.davils.kreate.standard.provider.ietf.RfcProvider
import com.davils.kreate.standard.provider.iso.IsoProvider
import com.davils.kreate.standard.provider.suite.SuiteProvider
import com.davils.kreate.standard.provider.unicode.UnicodeProvider
import com.davils.kreate.standard.provider.w3c.W3cProvider

internal object StandardProviders {
    fun all(context: ProviderContext, declarations: List<StandardDeclaration>): List<StandardProvider> = listOf(
        RfcProvider(context),
        DraftProvider(context),
        IsoProvider(context, declarations),
        W3cProvider(context, declarations),
        WhatwgProvider(context),
        UnicodeProvider(context),
        GitHubProvider(context),
        SuiteProvider(context),
        DocumentProvider(context)
    )
}
