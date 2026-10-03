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

package com.davils.kreate.rules.structure

import com.davils.kreate.rules.RULE_DOCUMENTATION
import dev.detekt.api.ActiveByDefault
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtFile

@ActiveByDefault(since = "4.0.0")
internal class OneTopLevelTypePerFile(config: Config) : Rule(
    config,
    "A file holds one top-level type and is named after it.",
    RULE_DOCUMENTATION
) {

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)

        if (file.isScript()) return

        val types = file.declarations.filterIsInstance<KtClassLikeDeclaration>()
        val fileName = file.name.substringBefore(NAME_SEPARATOR)
        val primary = types.firstOrNull()
        if (primary == null) {
            checkFunctionFileName(file, fileName)
            return
        }

        checkPrimaryName(primary, fileName)
        types.drop(1).forEach { additional -> reportAdditional(additional) }
    }

    private fun checkFunctionFileName(file: KtFile, fileName: String) {
        if (PASCAL_CASE.matches(fileName)) return

        report(
            Finding(
                Entity.from(file),
                "Rename '$fileName' to PascalCase after the role its declarations share."
            )
        )
    }

    private fun checkPrimaryName(primary: KtClassLikeDeclaration, fileName: String) {
        val typeName = primary.name ?: return
        if (typeName == fileName) return

        report(
            Finding(
                Entity.from(primary),
                "Rename the file '$fileName' to '$typeName', the type it holds."
            )
        )
    }

    private fun reportAdditional(additional: KtClassLikeDeclaration) {
        report(
            Finding(
                Entity.from(additional),
                "Move '${additional.name}' into a file of its own. A file holds one top-level type."
            )
        )
    }

    private companion object {
        private const val NAME_SEPARATOR: Char = '.'
        private val PASCAL_CASE: Regex = Regex("""[A-Z][A-Za-z0-9]*""")
    }
}
