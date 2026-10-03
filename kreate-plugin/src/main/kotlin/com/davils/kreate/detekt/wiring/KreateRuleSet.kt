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

package com.davils.kreate.detekt.wiring

import com.davils.kreate.KREATE_VERSION
import com.davils.kreate.UNKNOWN_KREATE_VERSION
import com.davils.kreate.detekt.DetektExtension
import org.gradle.api.Project

private const val DETEKT_PLUGINS_CONFIGURATION: String = "detektPlugins"

private const val RULE_SET_MODULE: String = "com.davils:kreate-detekt-rules"

internal fun Project.addKreateRuleSet(extension: DetektExtension) {
    if (!extension.kreateRules.get()) return

    val notation = kreateRuleSetNotation(KREATE_VERSION)
    if (notation == null) {
        logger.info(
            "Kreate is not adding '$RULE_SET_MODULE' to '$DETEKT_PLUGINS_CONFIGURATION': it " +
                "cannot read its own version, which is the version the rule set is published at."
        )
        return
    }

    dependencies.add(DETEKT_PLUGINS_CONFIGURATION, notation)
}

internal fun kreateRuleSetNotation(kreateVersion: String): String? {
    if (kreateVersion == UNKNOWN_KREATE_VERSION) return null
    return "$RULE_SET_MODULE:$kreateVersion"
}
