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

package com.davils.kreate.rules.kdoc

import com.davils.kreate.rules.RULE_DOCUMENTATION
import dev.detekt.api.ActiveByDefault
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtDeclaration

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
        private const val SINCE_TAG: String = "since"
    }
}
