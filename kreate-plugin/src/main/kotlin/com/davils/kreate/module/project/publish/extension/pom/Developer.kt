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

package com.davils.kreate.module.project.publish.extension.pom

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Extension for configuring POM developers metadata.
 *
 * @param factory The object factory used for creating properties.
 * @since 1.0.0
 */
public abstract class PomDevelopersExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 1.0.0
     */
    factory: ObjectFactory
) {
    /**
     * Configuration for a single developer.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val developer: PomDeveloperExtension

    /**
     * Configures the [PomDeveloperExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun developer(action: Action<PomDeveloperExtension>) {
        action.execute(developer)
    }
}

/**
 * Extension for configuring a single POM developer.
 *
 * @param factory The object factory used for creating properties.
 * @since 1.0.0
 */
public abstract class PomDeveloperExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 1.0.0
     */
    factory: ObjectFactory
) {
    /**
     * The unique identifier of the developer.
     * @since 1.0.0
     */
    public val id: Property<String> = factory.property(String::class.java)

    /**
     * The full name of the developer.
     * @since 1.0.0
     */
    public val name: Property<String> = factory.property(String::class.java)

    /**
     * The email address of the developer.
     * @since 1.0.0
     */
    public val email: Property<String> = factory.property(String::class.java)

    /**
     * The organization the developer belongs to.
     * @since 1.0.0
     */
    public val organization: Property<String> = factory.property(String::class.java)

    /**
     * The timezone of the developer.
     * @since 1.0.0
     */
    public val timezone: Property<String> = factory.property(String::class.java)
}