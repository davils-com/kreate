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
 * Contributes the Kreate comment and KDoc rules to Detekt.
 *
 * Detekt finds this class through `META-INF/services/dev.detekt.api.RuleSetProvider`, so the rules
 * run as soon as the artifact is on a `detektPlugins` configuration. Kreate puts it there for a
 * project that has Detekt enabled; nothing else has to be declared.
 *
 * Every rule in the set reports rather than rewrites. That is deliberate and it is the whole
 * difference between this rule set and the script it replaces: a comment is removed by the person
 * who knows whether the sentence it holds belongs in a name, in a test, or nowhere.
 *
 * @since 3.3.0
 */
public class KreateRuleSetProvider : RuleSetProvider {

    override val ruleSetId: RuleSetId = RuleSetId(KREATE_RULE_SET_ID)

    override fun instance(): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            ::ForbiddenLineComment,
            ::KDocOnNonPublicDeclaration,
            ::KDocWithoutSinceTag,
            ::SingleLineKDocWithBlockTag,
            ::KDocClosingMarkerOnSharedLine,
            ::KDocSeparatedFromDeclaration
        )
    )
}
