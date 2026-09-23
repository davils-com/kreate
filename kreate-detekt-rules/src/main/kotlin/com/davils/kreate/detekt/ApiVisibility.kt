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

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents

/**
 * How far a declaration reaches beyond the module that declares it.
 *
 * This is the distinction the documentation policy is built on, and it is coarser than Kotlin's
 * four visibilities on purpose: what matters is whether a caller outside the module can see the
 * declaration, not which keyword hid it.
 *
 * @since 3.3.0
 */
internal enum class ApiVisibility {
    /**
     * Visible to every consumer of the artifact.
     *
     * @since 3.3.0
     */
    PUBLIC,

    /**
     * Visible only to a subclass, and to nothing else.
     *
     * Kept apart from [PUBLIC] and [HIDDEN] because opinions differ on whether a `protected`
     * member is part of the surface a library documents, and a rule that cannot be configured on
     * that point is a rule that gets switched off.
     *
     * @since 3.3.0
     */
    PROTECTED,

    /**
     * Invisible outside the module: `private`, `internal`, or local to a function body.
     *
     * @since 3.3.0
     */
    HIDDEN
}

/**
 * The reach of this declaration, taking the declarations that enclose it into account.
 *
 * A `public` member of an `internal` class is not public, and neither is anything declared inside a
 * function body. Both are the cases a rule that only read the declaration's own modifier would
 * wave through.
 *
 * Constructors are skipped while walking outwards. A `private constructor` restricts who may build
 * the type, not who may see the properties declared in its parameter list: in
 * `public class Key private constructor(public val id: String)` the property is as public as the
 * class, and its KDoc belongs there.
 *
 * @return The visibility the declaration effectively has.
 * @since 3.3.0
 */
internal fun KtDeclaration.apiVisibility(): ApiVisibility {
    if (getStrictParentOfType<KtBlockExpression>() != null) return ApiVisibility.HIDDEN

    val enclosing = parents
        .filterIsInstance<KtDeclaration>()
        .filterNot { declaration -> declaration is KtConstructor<*> }
    val chain = listOf(this) + enclosing

    if (chain.any { declaration -> declaration.isPrivateOrInternal() }) return ApiVisibility.HIDDEN
    if (chain.any { declaration -> declaration.hasModifier(KtTokens.PROTECTED_KEYWORD) }) {
        return ApiVisibility.PROTECTED
    }

    return ApiVisibility.PUBLIC
}

/**
 * Whether this declaration carries a modifier that keeps it inside its module.
 *
 * @return `true` for `private` and `internal`.
 * @since 3.3.0
 */
private fun KtDeclaration.isPrivateOrInternal(): Boolean =
    hasModifier(KtTokens.PRIVATE_KEYWORD) || hasModifier(KtTokens.INTERNAL_KEYWORD)
