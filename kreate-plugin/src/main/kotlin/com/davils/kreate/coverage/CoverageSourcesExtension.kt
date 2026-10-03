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

package com.davils.kreate.coverage

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Selects which of the project's source sets contribute code to the coverage measurement.
 *
 * This decides what counts as the denominator. Code in an excluded source set is not measured
 * at all, which is the difference between "this code is untested" and "this code is not part of
 * the number" — a distinction that matters for generated sources and for source sets that exist
 * only to support the tests.
 *
 * @param factory The object factory used for creating properties.
 * @since 2.2.0
 */
public abstract class CoverageSourcesExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 2.2.0
     */
    factory: ObjectFactory
) {
    /**
     * The names of the source sets to measure.
     *
     * Empty by default, which means every source set the Kotlin plugin registers except those
     * listed in [excludedSourceSets].
     *
     * @since 2.2.0
     */
    public val includedSourceSets: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())

    /**
     * The names of the source sets to leave out of the measurement.
     *
     * Test source sets are already excluded by the coverage engine, so this is for the cases it
     * cannot know about — generated code, fixtures, or a source set that exists only to support
     * another module's build.
     *
     * @since 2.2.0
     */
    public val excludedSourceSets: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())

    /**
     * Whether Java sources are left out of the measurement.
     *
     * Defaults to `false`. In a mixed Kotlin and Java project, excluding Java produces a number
     * that describes only part of the artifact you ship, so this should be a deliberate choice.
     *
     * @since 2.2.0
     */
    public val excludeJava: Property<Boolean> =
        factory.property(Boolean::class.java).convention(false)
}
