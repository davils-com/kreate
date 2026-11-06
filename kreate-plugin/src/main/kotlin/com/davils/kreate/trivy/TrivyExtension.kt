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

package com.davils.kreate.trivy

import com.davils.kreate.trivy.provision.TrivyPins
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
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
 * @since 1.2.0
 */
public abstract class TrivyExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * Whether the Trivy module is enabled for this project.
     *
     * @since 1.2.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The exact Trivy version to use. Defaults to the version this Kreate release pins.
     *
     * Kreate holds the SHA-256 of the default version's releases, which is what protects a build from
     * a compromised release. Another version needs [checksums] for the same protection.
     *
     * @since 4.0.0
     */
    public val version: Property<String> = factory.property(String::class.java).convention(TrivyPins.DEFAULT_VERSION)

    /**
     * Whether any Trivy on this machine may be used instead of the pinned one. Defaults to `false`.
     *
     * Results differ between versions, so a build stays on the pinned one unless this says otherwise.
     *
     * @since 4.0.0
     */
    public val useInstalled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The Trivy executable to run, instead of finding or downloading one.
     *
     * The Gradle property `kreate.trivy.executable` overrides it.
     *
     * @since 4.0.0
     */
    public val executable: Property<String> = factory.property(String::class.java)

    /**
     * The SHA-256 of the release archive per platform, such as `Linux-64bit`, for a version other
     * than the default.
     *
     * @since 4.0.0
     */
    public val checksums: MapProperty<String, String> =
        factory.mapProperty(String::class.java, String::class.java).convention(emptyMap())

    /**
     * Configuration for scanning the project for insecure configuration.
     *
     * @since 4.0.0
     */
    @get:Nested
    public abstract val misconfig: TrivyDirectoryScanExtension

    /**
     * Configuration for one scan of the whole project directory.
     *
     * @since 4.0.0
     */
    @get:Nested
    public abstract val filesystem: TrivyDirectoryScanExtension

    /**
     * Configuration for vulnerability scanning (CVEs).
     *
     * @since 1.2.0
     */
    @get:Nested
    public abstract val vulnerability: TrivyVulnerabilityExtension

    /**
     * Configuration for license scanning.
     *
     * @since 1.2.0
     */
    @get:Nested
    public abstract val license: TrivyLicenseExtension

    /**
     * Configuration for secret scanning.
     *
     * @since 1.2.0
     */
    @get:Nested
    public abstract val secrets: TrivySecretExtension

    /**
     * Configures vulnerability scanning using the provided action.
     *
     * @param action The configuration action for [TrivyVulnerabilityExtension].
     * @since 1.2.0
     */
    public fun vulnerability(action: Action<TrivyVulnerabilityExtension>) {
        action.execute(vulnerability)
    }

    /**
     * Configures license scanning using the provided action.
     *
     * @param action The configuration action for [TrivyLicenseExtension].
     * @since 1.2.0
     */
    public fun license(action: Action<TrivyLicenseExtension>) {
        action.execute(license)
    }

    /**
     * Configures secret scanning using the provided action.
     *
     * @param action The configuration action for [TrivySecretExtension].
     * @since 1.2.0
     */
    public fun secrets(action: Action<TrivySecretExtension>) {
        action.execute(secrets)
    }

    /**
     * Configures the scan for insecure configuration.
     *
     * @param action The configuration action.
     * @since 4.0.0
     */
    public fun misconfig(action: Action<TrivyDirectoryScanExtension>) {
        action.execute(misconfig)
    }

    /**
     * Configures the scan of the whole project directory.
     *
     * @param action The configuration action.
     * @since 4.0.0
     */
    public fun filesystem(action: Action<TrivyDirectoryScanExtension>) {
        action.execute(filesystem)
    }
}
