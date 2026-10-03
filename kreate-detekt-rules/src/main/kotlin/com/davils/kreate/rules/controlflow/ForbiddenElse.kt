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

package com.davils.kreate.rules.controlflow

import com.davils.kreate.rules.RULE_DOCUMENTATION
import dev.detekt.api.ActiveByDefault
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtWhenEntry

@ActiveByDefault(since = "4.0.0")
internal class ForbiddenElse(config: Config) : Rule(
    config,
    "An else branch hides the case a guard clause would have named and left early.",
    RULE_DOCUMENTATION
) {

    override fun visitIfExpression(expression: KtIfExpression) {
        super.visitIfExpression(expression)

        val elseKeyword = expression.elseKeyword ?: return

        report(
            Finding(
                Entity.from(elseKeyword),
                "Replace this else branch with an early return: handle the exceptional case " +
                    "first, leave the function, and keep the main path at the left margin."
            )
        )
    }

    override fun visitWhenEntry(entry: KtWhenEntry) {
        super.visitWhenEntry(entry)

        if (!entry.isElse) return

        report(
            Finding(
                Entity.from(entry),
                "Remove this 'else ->' branch. A when over a sealed type or an enum is exhaustive " +
                    "without it; any other subject becomes a lookup or a when statement followed " +
                    "by an early return."
            )
        )
    }
}
