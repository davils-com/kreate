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
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtDeclaration

/**
 * Reports a KDoc block on a declaration that no consumer of the artifact can see.
 *
 * KDoc is the contract of a published API. On an `internal` or `private` declaration it documents
 * nothing anyone can call, it is not rendered by Dokka, and it is the documentation most likely to
 * drift, because no consumer ever reads it and notices that it is wrong. The reasoning behind such
 * a declaration belongs in the code that reads it — a name, a smaller function, a test.
 *
 * The visibility considered is the effective one: a `public` member of an `internal` class is
 * reported, and so is anything declared inside a function body.
 *
 * Detekt's `DocumentationOverPrivateFunction` and `DocumentationOverPrivateProperty` cover a part
 * of this — `private` functions and properties. This rule covers every declaration kind and adds
 * `internal`, which is where a multi-module Kotlin codebase keeps most of its implementation.
 *
 * @since 3.3.0
 */
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
        if (visibility == ApiVisibility.PROTECTED && allowProtected) return

        report(
            Finding(
                Entity.from(documentation),
                "Remove this KDoc block: the declaration it documents is " +
                    "${visibility.name.lowercase()}, so no consumer can call it. Anything worth " +
                    "saying about it is worth saying in a name, a smaller function or a test."
            )
        )
    }
}
