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

package com.davils.kreate.publish

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Extension for publishing to a Maven repository that keeps releases and snapshots apart.
 *
 * A version ending in `-SNAPSHOT` is published to [snapshots], every other version to
 * [releases]. Kreate never falls back to the other repository: when the URL for the kind of
 * version at hand is not declared, nothing is uploaded.
 *
 * @param factory The object factory used for creating properties.
 * @since 4.2.0
 */
public abstract class MavenTargetExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 4.2.0
     */
    factory: ObjectFactory
) {
    /**
     * Whether publishing to the Maven repository is enabled.
     * Defaults to `false`.
     * @since 4.2.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The name of the repository, which names the publish tasks, such as
     * `publishAllPublicationsToMavenRepository`.
     * Defaults to `Maven`.
     * @since 4.2.0
     */
    public val name: Property<String> = factory.property(String::class.java).convention("Maven")

    /**
     * The URL release versions are published to.
     * @since 4.2.0
     */
    public val releases: Property<String> = factory.property(String::class.java)

    /**
     * The URL versions ending in `-SNAPSHOT` are published to.
     * @since 4.2.0
     */
    public val snapshots: Property<String> = factory.property(String::class.java)

    /**
     * The environment variable that holds the user name.
     * Defaults to `MAVEN_USERNAME`.
     * @since 4.2.0
     */
    public val usernameEnv: Property<String> = factory.property(String::class.java).convention("MAVEN_USERNAME")

    /**
     * The environment variable that holds the password or token.
     * Defaults to `MAVEN_PASSWORD`.
     * @since 4.2.0
     */
    public val passwordEnv: Property<String> = factory.property(String::class.java).convention("MAVEN_PASSWORD")

    /**
     * Whether to sign all publications.
     * Defaults to `false`.
     * @since 4.2.0
     */
    public val signPublications: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * Whether a plain `http` URL is accepted.
     * Defaults to `false`.
     * @since 4.2.0
     */
    public val allowInsecureProtocol: Property<Boolean> = factory.property(Boolean::class.java).convention(false)
}
