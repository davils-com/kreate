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

package com.davils.kreate.module.platform

import com.davils.kreate.module.platform.jvm.JvmExtension
import com.davils.kreate.module.platform.multiplatform.MultiplatformExtension
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Extension for configuring platform-specific settings in Kreate.
 *
 * This extension provides properties for Java version, Kotlin API mode,
 * and warning handling, as well as nested configuration for multiplatform.
 *
 * @param factory The object factory used for creating properties.
 * @since 1.0.0
 */
public abstract class PlatformExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 1.0.0
     */
    factory: ObjectFactory
) {
    /**
     * The Java version to be used for the project.
     * Defaults to [JavaVersion.VERSION_25].
     * @since 1.0.0
     */
    public val javaVersion: Property<JavaVersion> = factory.property(
        JavaVersion::class.java
    ).convention(JavaVersion.VERSION_25)

    /**
     * Whether to enable explicit API mode in Kotlin.
     * Defaults to `true`.
     * @since 1.0.0
     */
    public val explicitApi: Property<Boolean> = factory.property(
        Boolean::class.java
    ).convention(true)

    /**
     * Whether to treat all compiler warnings as errors.
     * Defaults to `false`.
     * @since 1.0.0
     */
    public val allWarningsAsErrors: Property<Boolean> = factory.property(
        Boolean::class.java
    ).convention(false)

    /**
     * Configuration for Kotlin Multiplatform.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val multiplatform: MultiplatformExtension

    /**
     * Configuration for JVM targets.
     * @since 1.1.0
     * */
    @get:Nested
    public abstract val jvm: JvmExtension

    /**
     * Configures the [MultiplatformExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun multiplatform(action: Action<MultiplatformExtension>) {
        action.execute(multiplatform)
    }

    /**
     * Configures the [JvmExtension] using the provided action.
     * @since 1.1.0
     * */
    public fun jvm(action: Action<JvmExtension>) {
        action.execute(jvm)
    }
}
