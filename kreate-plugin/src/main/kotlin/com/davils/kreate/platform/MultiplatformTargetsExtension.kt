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

package com.davils.kreate.platform

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

/**
 * The targets Kreate declares on a Kotlin Multiplatform project.
 *
 * Kreate declares them as soon as the `targets { }` block has run, while the build script is still
 * evaluated, so the Kotlin plugin applies compiler plugins such as KSP or kotlinx-serialization to
 * them like to targets declared in `kotlin { }`. Once the build script is evaluated, Kreate declares
 * the targets again before any other feature reads them, which takes up values set outside the
 * block; a target first declared there misses the compiler plugins. Declaring a target that already
 * exists, here or in `kotlin { }`, configures the existing one.
 *
 * Setting a target on a project with the Kotlin JVM plugin fails the build, and on a project without
 * a Kotlin plugin it has no effect.
 *
 * @param factory The object factory used for creating properties.
 * @since 5.1.0
 */
public abstract class MultiplatformTargetsExtension @Inject constructor(
    factory: ObjectFactory
) {
    /**
     * Whether Kreate declares the `jvm` target. Defaults to `false`.
     *
     * @since 5.1.0
     */
    public val jvm: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The environments of the `js` target, compiled with the IR backend. Empty by default, which
     * declares no `js` target.
     *
     * @since 5.1.0
     */
    public val js: SetProperty<WebEnvironment> =
        factory.setProperty(WebEnvironment::class.java).convention(emptySet())

    /**
     * The environments of the `wasmJs` target. Empty by default, which declares no `wasmJs` target.
     *
     * @since 5.1.0
     */
    public val wasmJs: SetProperty<WebEnvironment> =
        factory.setProperty(WebEnvironment::class.java).convention(emptySet())

    private var declaration: Action<MultiplatformTargetsExtension>? = null

    internal fun declareWith(action: Action<MultiplatformTargetsExtension>) {
        declaration = action
    }

    internal fun declareNow() {
        declaration?.execute(this)
    }
}
