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
package com.davils.kreate.api

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configuration of the semantic versioning check, which compares the API dump with the one committed
 * at the last release and checks that the project version is high enough for the difference.
 *
 * @param factory The object factory used to create the properties.
 * @since 4.0.0
 */
public abstract class ApiSemverExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * Whether `kreateApiSemverCheck` is registered. Defaults to `false`.
     *
     * @since 4.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The revision to compare with. Defaults to the highest release tag reachable from `HEAD`.
     *
     * @since 4.0.0
     */
    public val baseline: Property<String> = factory.property(String::class.java)

    /**
     * The prefix of release tags, such as `v`. Defaults to none.
     *
     * @since 4.0.0
     */
    public val tagPrefix: Property<String> = factory.property(String::class.java).convention("")

    /**
     * Whether a breaking change also needs a commit marked as breaking since the baseline.
     * Defaults to `true`.
     *
     * @since 4.0.0
     */
    public val requireBreakingCommit: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * Whether `check` runs `kreateApiSemverCheck`. Defaults to `true`.
     *
     * @since 4.0.0
     */
    public val runOnCheck: Property<Boolean> = factory.property(Boolean::class.java).convention(true)
}
