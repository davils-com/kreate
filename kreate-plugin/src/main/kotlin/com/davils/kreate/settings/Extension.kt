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

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * Extension for configuring Kreate settings in `settings.gradle.kts`.
 *
 * This extension allows managing Git-based source dependencies and other settings-level
 * configurations.
 *
 * @param factory The [ObjectFactory] used to create instances of configuration objects.
 * @since 1.0.0
 */
public abstract class KreateSettingsExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * Container for Git dependencies.
     *
     * Allows defining multiple Git repositories as source dependencies.
     *
     * @since 1.0.0
     */
    public val git: NamedDomainObjectContainer<GitDependency> = factory.domainObjectContainer(GitDependency::class.java)

    /**
     * Configures Git dependencies.
     *
     * @param action The configuration action for the Git dependencies container.
     * @since 1.0.0
     */
    public fun git(action: Action<NamedDomainObjectContainer<GitDependency>>) {
        action.execute(git)
    }
}