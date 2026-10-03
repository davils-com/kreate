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

package com.davils.kreate.rules

import com.davils.kreate.rules.comment.ForbiddenBlockComment
import com.davils.kreate.rules.comment.ForbiddenLineComment
import com.davils.kreate.rules.controlflow.ForbiddenElse
import com.davils.kreate.rules.expression.ChainedCallLimit
import com.davils.kreate.rules.kdoc.KDocClosingMarkerOnSharedLine
import com.davils.kreate.rules.kdoc.KDocOnNonPublicDeclaration
import com.davils.kreate.rules.kdoc.KDocSeparatedFromDeclaration
import com.davils.kreate.rules.kdoc.KDocWithoutSinceTag
import com.davils.kreate.rules.kdoc.SingleLineKDocWithBlockTag
import com.davils.kreate.rules.structure.OneTopLevelTypePerFile
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/**
 * The id the rule set is configured under in `detekt.yml`.
 *
 * @since 3.3.0
 */
public const val KREATE_RULE_SET_ID: String = "kreate"

/**
 * Contributes the Kreate code style rules to Detekt.
 *
 * Detekt finds this class through `META-INF/services/dev.detekt.api.RuleSetProvider`, so the rules
 * run as soon as the artifact is on a `detektPlugins` configuration. Kreate puts it there for a
 * project that has Detekt enabled; nothing else has to be declared.
 *
 * Every rule in the set reports rather than rewrites. A comment, an `else` branch or a long call
 * chain is resolved by the person who knows which name or which early return the code is missing.
 *
 * @since 3.3.0
 */
public class KreateRuleSetProvider : RuleSetProvider {

    override val ruleSetId: RuleSetId = RuleSetId(KREATE_RULE_SET_ID)

    override fun instance(): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            ::ForbiddenLineComment,
            ::ForbiddenBlockComment,
            ::ForbiddenElse,
            ::ChainedCallLimit,
            ::OneTopLevelTypePerFile,
            ::KDocOnNonPublicDeclaration,
            ::KDocWithoutSinceTag,
            ::SingleLineKDocWithBlockTag,
            ::KDocClosingMarkerOnSharedLine,
            ::KDocSeparatedFromDeclaration
        )
    )
}
