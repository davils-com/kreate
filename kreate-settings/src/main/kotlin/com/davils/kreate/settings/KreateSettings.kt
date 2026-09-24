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

package com.davils.kreate.settings

import com.davils.kreate.module.local.LocalMode
import com.davils.kreate.module.local.gatherLocalModeInputs
import com.davils.kreate.module.local.mavenLocalOf
import com.davils.kreate.module.local.resolveLocalMode
import com.davils.kreate.module.local.workspace
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.create

/**
 * Makes a build resolve artifacts that a sibling checkout published locally.
 *
 * ```kotlin
 * // settings.gradle.kts
 * plugins {
 *     id("com.davils.kreate.settings") version "3.2.0"
 * }
 * ```
 *
 * This is the consumer half of the local development workflow. The project plugin
 * `com.davils.kreate` publishes; this one resolves. A repository needs both, and needs this one
 * in its `build-logic/settings.gradle.kts` as well — that is where the Kreate plugin marker
 * itself is resolved, and `build-logic` does not apply the project plugin.
 *
 * ### Why a settings plugin
 *
 * Dependency substitution has to be installed before anything resolves, and it has to reach every
 * project of the build including ones that never apply Kreate. A project plugin can guarantee
 * neither. The Kotlin Multiplatform and Android plugins both resolve during their own
 * `afterEvaluate`, so a project plugin doing this would work on most days — which is not a
 * standard dependency resolution can be held to.
 *
 * ### Nothing happens until something is published
 *
 * Applying this plugin does not change how a build resolves. It changes only what happens *after*
 * a `kreateLocalPublish` somewhere on the same machine, and reverts the moment
 * `kreateLocalClean` runs. In CI, where the state directory lives under a `GRADLE_USER_HOME` that
 * is recreated for every job, it can do nothing at all.
 *
 * @since 3.2.0
 */
public class KreateSettings : Plugin<Settings> {
    /**
     * Applies the plugin to the given settings.
     *
     * @param settings The settings to configure.
     * @since 3.2.0
     */
    override fun apply(settings: Settings) {
        val extension = settings.extensions.create<KreateSettingsExtension>("kreateSettings")

        // Deferred to `settingsEvaluated` because a `plugins { }` block is applied before the rest
        // of the settings script has run: reading the extension here would read its defaults and
        // silently ignore a `kreateSettings { }` block written three lines further down.
        settings.gradle.settingsEvaluated {
            settings.installLocalResolution(extension)
        }
    }
}

/**
 * Installs the local resolution lifecycle actions, if local mode is on.
 *
 * @param extension The settings extension.
 * @since 3.2.0
 */
private fun Settings.installLocalResolution(extension: KreateSettingsExtension) {
    if (!extension.enabled.get()) return

    val mode = resolveLocalMode(
        gatherLocalModeInputs(
            providers,
            gradle.gradleUserHomeDir,
            extension.ciEnvironmentVariables.get()
        )
    )
    if (mode !is LocalMode.Active) return

    val repository = mavenLocalOf(providers)

    gradle.lifecycle.beforeProject(
        LocalResolutionAction(
            substitutions = HashMap(mode.workspace.substitutions),
            repositoryName = extension.repositoryName.get(),
            repositoryUri = repository.toURI().toString()
        )
    )

    // After, not before. See `DeactivateLockingAction` — the ordering is the entire point.
    gradle.lifecycle.afterProject(DeactivateLockingAction())

    announceLocalMode(mode, repository)
}
