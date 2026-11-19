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
package com.davils.kreate.standard.provider.unicode

import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.outcome.flatMap
import com.davils.kore.core.parseIntOrNull
import com.davils.kreate.standard.fetch.FetchRequest
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardKind
import com.davils.kreate.standard.model.StandardSnapshot
import com.davils.kreate.standard.model.UNICODE_DATABASE
import com.davils.kreate.standard.provider.ProviderContext
import com.davils.kreate.standard.provider.StandardProvider

private const val MAX_PAGE_BYTES: Int = 4 * 1024 * 1024

internal class UnicodeProvider(private val context: ProviderContext) : StandardProvider {
    override fun accepts(declaration: StandardDeclaration): Boolean = declaration.id.kind == StandardKind.UNICODE

    override fun snapshotOf(declaration: StandardDeclaration): Outcome<StandardSnapshot, LookupFailure> {
        val name = declaration.id.name
        if (name == UNICODE_DATABASE) return database()
        val number = parseIntOrNull(name.substringAfter('-'))
            ?: return Outcome.Failure(LookupFailure.Unknown("$name names no Unicode report."))
        return report(name, number)
    }

    private fun database(): Outcome<StandardSnapshot, LookupFailure> {
        val uri = context.endpoints.resolve(context.endpoints.unicode, "Public/UCD/latest/ReadMe.txt")
        val readMe = context.fetch(FetchRequest(uri, MAX_PAGE_BYTES))
        return readMe.flatMap { document -> databaseOf(document.text()) }
    }

    private fun databaseOf(text: String): Outcome<StandardSnapshot, LookupFailure> {
        val version = UnicodePages.databaseVersionOf(text)
            ?: return unreadable("the ReadMe of the Unicode Character Database")
        return Outcome.Success(StandardSnapshot(title = "Unicode Character Database", revision = version))
    }

    private fun report(name: String, number: Int): Outcome<StandardSnapshot, LookupFailure> {
        val uri = context.endpoints.resolve(context.endpoints.unicode, "reports/tr$number/")
        val page = context.fetch(FetchRequest(uri, MAX_PAGE_BYTES))
        return page.flatMap { document -> reportOf(name, number, document.text()) }
    }

    private fun reportOf(name: String, number: Int, text: String): Outcome<StandardSnapshot, LookupFailure> {
        val revision = UnicodePages.revisionOf(text, number) ?: return unreadable("the page of $name")
        val title = UnicodePages.titleOf(text) ?: name.uppercase()
        val snapshot = StandardSnapshot(title = title, revision = revision, published = UnicodePages.dateOf(text))
        return Outcome.Success(snapshot)
    }

    private fun unreadable(what: String): Outcome<StandardSnapshot, LookupFailure> =
        Outcome.Failure(LookupFailure.Unavailable("Kreate could not read the version from $what."))
}
