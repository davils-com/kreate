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
 * Extension for configuring POM licenses metadata.
 *
 * @since 1.0.0
 */
public abstract class PomLicensesExtension @Inject constructor() {
    /**
     * Configuration for a single license.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val license: PomLicenseExtension

    /**
     * Configures the [PomLicenseExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun license(action: Action<PomLicenseExtension>) {
        action.execute(license)
    }
}

/**
 * Extension for configuring a single POM license.
 *
 * @param factory The object factory used for creating properties.
 * @since 1.0.0
 */
public abstract class PomLicenseExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 1.0.0
     */
    factory: ObjectFactory
) {
    /**
     * The name of the license (e.g., "The Apache License, Version 2.0").
     * @since 1.0.0
     */
    public val name: Property<String> = factory.property(String::class.java)

    /**
     * The URL to the license text.
     * @since 1.0.0
     */
    public val url: Property<String> = factory.property(String::class.java)

    /**
     * The distribution of the license (e.g., "repo").
     * @since 1.0.0
     */
    public val distribution: Property<String> = factory.property(String::class.java)
}
