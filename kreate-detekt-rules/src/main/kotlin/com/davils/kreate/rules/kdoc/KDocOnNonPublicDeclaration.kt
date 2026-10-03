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
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtDeclaration

@ActiveByDefault(since = "3.3.0")
internal class KDocOnNonPublicDeclaration(config: Config) : Rule(
    config,
    "KDoc documents a published API, and this declaration is not part of one.",
    RULE_DOCUMENTATION
) {

    @Configuration(
        "Whether a 'protected' declaration may carry KDoc. It is only visible to a subclass, and " +
            "the Kreate standard documents the public surface alone."
    )
    private val allowProtected: Boolean by config(false)

    override fun visitDeclaration(dcl: KtDeclaration) {
        super.visitDeclaration(dcl)

        val documentation = dcl.docComment ?: return
        val visibility = dcl.apiVisibility()
        if (visibility == ApiVisibility.PUBLIC) return

        val isAllowedProtected = visibility == ApiVisibility.PROTECTED && allowProtected
        if (isAllowedProtected) return

        val visibilityName = visibility.name.lowercase()
        report(
            Finding(
                Entity.from(documentation),
                "Remove this KDoc block: the declaration it documents is $visibilityName, so no " +
                    "consumer can call it. Anything worth saying about it is worth saying in a " +
                    "name, a smaller function or a test."
            )
        )
    }
}
