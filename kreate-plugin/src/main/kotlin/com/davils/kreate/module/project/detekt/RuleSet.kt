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

package com.davils.kreate.module.project.detekt

import com.davils.kreate.module.local.KREATE_VERSION
import com.davils.kreate.module.local.UNKNOWN_KREATE_VERSION
import com.davils.kreate.module.project.detekt.extension.DetektExtension
import org.gradle.api.Project

/**
 * The configuration Detekt loads its rule set plugins from.
 *
 * @since 3.3.0
 */
private const val DETEKT_PLUGINS_CONFIGURATION: String = "detektPlugins"

/**
 * The coordinates of the rule set that carries the Kreate comment and KDoc rules.
 *
 * Published from this repository alongside the plugin and versioned with it, so the rules a project
 * is analysed by are the ones the Kreate it applies was built with. A rule set resolved at a version
 * of its own would drift from the plugin that configures it, and the first sign of the drift would
 * be a finding nobody can reproduce.
 *
 * @since 3.3.0
 */
private const val RULE_SET_MODULE: String = "com.davils:kreate-detekt-rules"

/**
 * Puts the Kreate rule set on Detekt's analysis classpath.
 *
 * The rules report; they never rewrite. What they enforce is the comment and KDoc part of the Kreate
 * Kotlin standard — no `//` comments, KDoc on the published surface only, `@since` on everything
 * documented — and the rule set's own default configuration activates them, so a project that builds
 * upon Detekt's default configuration needs no further entry in its `detekt.yml`.
 *
 * Nothing is added when Kreate cannot name its own version. That happens when the plugin is loaded
 * from a directory of classes rather than from a JAR, which is the case in Kreate's own functional
 * tests and in a build that puts the plugin on its script classpath by hand. Guessing a version
 * there would fail resolution with a message about a missing artifact rather than about the guess.
 *
 * @param extension The Detekt configuration of the project.
 * @since 3.3.0
 */
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

/**
 * The dependency notation of the rule set for a given Kreate version.
 *
 * @param kreateVersion The version of Kreate that is running.
 * @return The notation, or `null` when the version is unknown and nothing can be pinned.
 * @since 3.3.0
 */
internal fun kreateRuleSetNotation(kreateVersion: String): String? {
    if (kreateVersion == UNKNOWN_KREATE_VERSION) return null
    return "$RULE_SET_MODULE:$kreateVersion"
}
