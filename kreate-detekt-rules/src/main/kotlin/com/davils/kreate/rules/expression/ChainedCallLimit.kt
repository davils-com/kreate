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

package com.davils.kreate.rules.expression

import com.davils.kreate.rules.RULE_DOCUMENTATION
import dev.detekt.api.ActiveByDefault
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression

@ActiveByDefault(since = "4.0.0")
internal class ChainedCallLimit(config: Config) : Rule(
    config,
    "A chain of calls hides every intermediate result a name would have explained.",
    RULE_DOCUMENTATION
) {

    @Configuration("The number of calls one qualified expression may chain.")
    private val maxCalls: Int by config(DEFAULT_MAX_CALLS)

    override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
        super.visitQualifiedExpression(expression)

        if (expression.continuesInParent()) return

        val calls = expression.chainedCalls()
        if (calls <= maxCalls) return

        report(
            Finding(
                Entity.from(expression),
                "This expression chains $calls calls, at most $maxCalls are allowed. Give the " +
                    "intermediate results names."
            )
        )
    }

    private fun KtQualifiedExpression.continuesInParent(): Boolean {
        val enclosing = generateSequence(chainParent()) { link -> link.chainParent() }
        return enclosing.any { link -> link is KtQualifiedExpression }
    }

    private fun KtExpression.chainParent(): KtExpression? {
        val enclosing = parent as? KtExpression ?: return null
        val continuesChain = enclosing.chainReceiver() == this
        if (!continuesChain) return null
        return enclosing
    }

    private fun KtQualifiedExpression.chainedCalls(): Int {
        val links = generateSequence<KtExpression>(this) { link -> link.chainReceiver() }
        return links.count { link -> link.invokesCall() }
    }

    private fun KtExpression.chainReceiver(): KtExpression? {
        if (this is KtQualifiedExpression) return receiverExpression
        if (this is KtArrayAccessExpression) return arrayExpression
        if (this is KtPostfixExpression) return baseExpression
        if (this is KtParenthesizedExpression) return expression
        return null
    }

    private fun KtExpression.invokesCall(): Boolean {
        if (this is KtQualifiedExpression) return selectorExpression is KtCallExpression
        return this is KtCallExpression
    }

    private companion object {
        private const val DEFAULT_MAX_CALLS: Int = 2
    }
}
