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

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import javax.inject.Inject

/**
 * Options of the Kotlin compiler that Kreate sets on every Kotlin compilation of the project.
 *
 * An option that is left unset keeps the value the Kotlin Gradle plugin or the build gives it.
 *
 * @param factory The object factory used to create the properties.
 * @since 4.3.0
 */
public abstract class CompilerExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * Whether the compiler runs in progressive mode, which fixes deprecations and bugs right away
     * instead of after a deprecation cycle. Defaults to `false`.
     *
     * @since 4.3.0
     */
    public val progressiveMode: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * Whether the compiler reports its additional warnings. Defaults to `false`.
     *
     * @since 4.3.0
     */
    public val extraWarnings: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The version of the Kotlin language the sources are compiled with. Unset by default.
     *
     * @since 4.3.0
     */
    public val languageVersion: Property<KotlinVersion> = factory.property(KotlinVersion::class.java)

    /**
     * The version of the Kotlin standard library API the sources may use. Unset by default.
     *
     * @since 4.3.0
     */
    public val apiVersion: Property<KotlinVersion> = factory.property(KotlinVersion::class.java)

    /**
     * How the JVM compilations emit default methods of interfaces. Unset by default.
     *
     * @since 4.3.0
     */
    public val jvmDefault: Property<JvmDefaultMode> = factory.property(JvmDefaultMode::class.java)
}
