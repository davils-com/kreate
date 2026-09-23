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

package com.davils.kreate.detekt

import dev.detekt.api.ActiveByDefault
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtDeclaration

/**
 * Reports a KDoc block on a public declaration that does not say which version introduced it.
 *
 * `@since` is the one tag a reader cannot reconstruct from the code. It answers the question every
 * consumer of a library asks before they use a declaration — whether the version they are pinned to
 * has it — and it is the tag that makes a deprecation cycle legible years later.
 *
 * Only public declarations are reported. A declaration that should not carry KDoc at all is the
 * business of [KDocOnNonPublicDeclaration], and reporting both on the same comment would say twice
 * that something is wrong while leaving it ambiguous what to do.
 *
 * @since 3.3.0
 */
@ActiveByDefault(since = "3.3.0")
internal class KDocWithoutSinceTag(config: Config) : Rule(
    config,
    "A documented public declaration has to name the version it appeared in.",
    RULE_DOCUMENTATION
) {

    override fun visitDeclaration(dcl: KtDeclaration) {
        super.visitDeclaration(dcl)

        val documentation = dcl.docComment ?: return
        if (dcl.apiVisibility() != ApiVisibility.PUBLIC) return
        if (documentation.hasBlockTag(SINCE_TAG)) return

        report(
            Finding(
                Entity.from(documentation),
                "Add an '@$SINCE_TAG <version>' tag. A consumer reading this KDoc cannot tell " +
                    "which release they need in order to call the declaration."
            )
        )
    }

    private companion object {
        /**
         * The tag that records the version a declaration appeared in.
         *
         * @since 3.3.0
         */
        private const val SINCE_TAG: String = "since"
    }
}
