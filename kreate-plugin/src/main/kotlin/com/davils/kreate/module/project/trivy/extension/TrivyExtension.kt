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

package com.davils.kreate.module.project.trivy.extension

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Main extension for configuring Trivy security scans.
 *
 * This extension provides high-level control over which Trivy scans are enabled
 * and allows for detailed configuration of vulnerability, license, and secret scanning.
 *
 * @param factory The object factory used for creating properties.
 * @since 1.0.0
 */
public abstract class TrivyExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * Whether the Trivy module is enabled for this project.
     *
     * @since 1.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * Configuration for vulnerability scanning (CVEs).
     *
     * @since 1.0.0
     */
    @get:Nested
    public abstract val vulnerability: TrivyVulnerabilityExtension

    /**
     * Configuration for license scanning.
     *
     * @since 1.0.0
     */
    @get:Nested
    public abstract val license: TrivyLicenseExtension

    /**
     * Configuration for secret scanning.
     *
     * @since 1.0.0
     */
    @get:Nested
    public abstract val secrets: TrivySecretExtension

    /**
     * Configures vulnerability scanning using the provided action.
     *
     * @param action The configuration action for [TrivyVulnerabilityExtension].
     * @since 1.0.0
     */
    public fun vulnerability(action: Action<TrivyVulnerabilityExtension>) {
        action.execute(vulnerability)
    }

    /**
     * Configures license scanning using the provided action.
     *
     * @param action The configuration action for [TrivyLicenseExtension].
     * @since 1.0.0
     */
    public fun license(action: Action<TrivyLicenseExtension>) {
        action.execute(license)
    }

    /**
     * Configures secret scanning using the provided action.
     *
     * @param action The configuration action for [TrivySecretExtension].
     * @since 1.0.0
     */
    public fun secrets(action: Action<TrivySecretExtension>) {
        action.execute(secrets)
    }
}
