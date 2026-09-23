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

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Extension for the settings half of the local development workflow.
 *
 * Configured in `settings.gradle.kts` through the `kreateSettings { }` block. Everything here has
 * a working default, and a repository that simply applies the plugin gets the intended behaviour
 * without writing the block at all.
 *
 * @param factory The object factory used for creating properties.
 * @since 3.2.0
 */
public abstract class KreateSettingsExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 3.2.0
     */
    factory: ObjectFactory
) {
    /**
     * Whether this build resolves locally published artifacts.
     *
     * Defaults to `true`. Switching it off here is a decision about the repository — a build that
     * must never resolve anything local, whatever a developer has installed. For switching it off
     * once, `-Pkreate.local=false` on the command line is the right instrument.
     *
     * @since 3.2.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * The name the injected local Maven repository is reported under.
     *
     * Defaults to `KreateLocal`. It appears in resolution errors and in `--info` output, and a
     * recognisable name is what turns a "could not find" into an obvious diagnosis.
     *
     * @since 3.2.0
     */
    public val repositoryName: Property<String> =
        factory.property(String::class.java).convention("KreateLocal")

    /**
     * The environment variables whose presence means the build is running in CI.
     *
     * Defaults to `CI`, `GITLAB_CI`, `GITHUB_ACTIONS` and `CI_PIPELINE_ID`. Must agree with the
     * project plugin's list: a build whose settings substituted a snapshot while its projects
     * still enforced lock files would fail in a way neither half explains.
     *
     * @since 3.2.0
     */
    public val ciEnvironmentVariables: ListProperty<String> = factory
        .listProperty(String::class.java)
        .convention(listOf("CI", "GITLAB_CI", "GITHUB_ACTIONS", "CI_PIPELINE_ID"))
}
