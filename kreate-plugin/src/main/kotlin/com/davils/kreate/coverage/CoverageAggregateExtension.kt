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
 * Merges the coverage of several projects into this project's reports.
 *
 * In a multi-project build, each project measures only itself by default. That produces one
 * number per module and none for the product, which is the number a release actually cares
 * about — and it hides the case where a class is untested in its own module but exercised
 * thoroughly by another module's tests. Aggregation is what turns per-module figures into a
 * single measurement of the whole.
 *
 * Enable it on the project that should own the combined report, usually the root:
 *
 * ```kotlin
 * kreate {
 *     project {
 *         coverage {
 *             enabled = true
 *
 *             aggregate {
 *                 enabled = true
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * Every aggregated project must have the Kover plugin applied itself. Kreate deliberately does
 * not apply it for them: Kover's own `merge { }` block does exactly that, and injecting a plugin
 * into a project whose build script never mentions it is the behaviour Kreate exists to avoid.
 * A project that is missing it is reported by path rather than by a resolution failure.
 *
 * @param factory The object factory used for creating properties.
 * @since 2.2.0
 */
public abstract class CoverageAggregateExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 2.2.0
     */
    factory: ObjectFactory
) {
    /**
     * Whether the coverage of other projects is merged into this project's reports.
     *
     * Defaults to `false`.
     *
     * @since 2.2.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The paths of the projects to merge, for example `:core` or `:service:api`.
     *
     * Empty by default, which merges every subproject of this one. Listing paths explicitly is
     * the safer choice in a build that gains modules over time: a new module then has to be
     * added deliberately rather than silently changing the number the gate is measured against.
     *
     * @since 2.2.0
     */
    public val projects: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())
}
