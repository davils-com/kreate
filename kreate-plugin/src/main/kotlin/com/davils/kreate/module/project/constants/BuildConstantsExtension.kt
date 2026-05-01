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

package com.davils.kreate.module.project.constants

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Extension for configuring build constants generation in Kreate.
 *
 * This extension allows defining key-value pairs that will be generated as a
 * Kotlin class (typically `BuildConstants`) available at compile time.
 *
 * @param factory The object factory used for creating properties.
 * @since 1.0.0
 */
public abstract class BuildConstantsExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 1.0.0
     */
    factory: ObjectFactory
) {
    private val properties: MapProperty<String, String> = factory.mapProperty(
        String::class.java, String::class.java
    ).convention(emptyMap())

    /**
     * Whether build constants generation is enabled.
     * Defaults to `false`.
     * @since 1.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * Optional override for the package name of the generated class.
     * @since 1.0.0
     */
    public val packageNameOverride: Property<String> = factory.property(String::class.java)

    /**
     * The name of the generated Kotlin class.
     * Defaults to "BuildConstants".
     * @since 1.0.0
     */
    public val className: Property<String> = factory.property(
        String::class.java
    ).convention("BuildConstants")

    /**
     * The output path for the generated source file.
     * Defaults to "generated/compile".
     * @since 1.0.0
     */
    public val path: Property<String> = factory.property(
        String::class.java
    ).convention("generated/compile")

    /**
     * Adds a constant to the generated class.
     *
     * @param key The name of the constant. Must not be blank.
     * @param value The value of the constant.
     * @throws IllegalArgumentException If the key is blank.
     * @since 1.0.0
     */
    public fun constant(key: String, value: String) {
        require(key.isNotBlank()) { "Key must not be blank" }
        properties.put(key, value)
    }

    /**
     * Adds a constant to the generated class, converting the value to a string.
     *
     * @param key The name of the constant. Must not be blank.
     * @param value The value of the constant.
     * @throws IllegalArgumentException If the key is blank.
     * @since 1.0.0
     */
    public fun constant(key: String, value: Any) {
        constant(key, value.toString())
    }

    /**
     * Retrieves the map of all defined constants.
     *
     * @return A map of constant names to their values.
     * @since 1.0.0
     */
    public fun getConstants(): Map<String, String> = properties.get().toMap()
}
