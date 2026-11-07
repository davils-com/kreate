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
package com.davils.kreate.assurance

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

private const val DEFAULT_FRESH_PACKAGE_DAYS: Int = 7

private const val DEFAULT_YOUNG_PACKAGE_DAYS: Int = 30

private const val DEFAULT_FRESH_RELEASE_HOURS: Int = 24

private const val DEFAULT_MIN_SCORECARD: Double = 4.0

/**
 * Configuration of the trust assessment, which reads the age of every dependency and the
 * OpenSSF Scorecard of its source repository from deps.dev.
 *
 * Modules of `internalGroups` are never sent.
 *
 * @param factory The object factory used to create the properties.
 * @since 4.0.0
 */
public abstract class AssuranceTrustExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * Whether the dependencies are assessed. Defaults to `false`.
     *
     * @since 4.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * A module first published fewer days ago is reported as `FRESH_PACKAGE`. Defaults to 7.
     *
     * @since 4.0.0
     */
    public val freshPackageDays: Property<Int> =
        factory.property(Int::class.java).convention(DEFAULT_FRESH_PACKAGE_DAYS)

    /**
     * A module first published fewer days ago is reported as `YOUNG_PACKAGE`. Defaults to 30.
     *
     * @since 4.0.0
     */
    public val youngPackageDays: Property<Int> =
        factory.property(Int::class.java).convention(DEFAULT_YOUNG_PACKAGE_DAYS)

    /**
     * A version published fewer hours ago is reported as `FRESH_RELEASE`. Defaults to 24.
     *
     * @since 4.0.0
     */
    public val freshReleaseHours: Property<Int> =
        factory.property(Int::class.java).convention(DEFAULT_FRESH_RELEASE_HOURS)

    /**
     * A source repository with a lower OpenSSF Scorecard is reported as `LOW_SCORECARD`. `0` turns
     * the check off. Defaults to 4.0 out of 10.
     *
     * @since 4.0.0
     */
    public val minScorecard: Property<Double> =
        factory.property(Double::class.java).convention(DEFAULT_MIN_SCORECARD)

    /**
     * Whether the plugins on the build script class path are assessed as well. Defaults to `true`.
     *
     * @since 4.0.0
     */
    public val includeBuildscript: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * The configurations to assess. Defaults to the classpaths of the main code.
     *
     * @since 4.0.0
     */
    public val configurations: SetProperty<String> = factory.setProperty(String::class.java)
}
