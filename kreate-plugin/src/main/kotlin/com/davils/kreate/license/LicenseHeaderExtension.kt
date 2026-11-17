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

package com.davils.kreate.license

import com.davils.kreate.license.header.apacheLicenseNotice
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

/**
 * Configuration of the license header check: the notice every file starts with and the files that
 * are checked.
 *
 * Kotlin, Java and build script files carry the notice in a block comment, property, YAML, TOML and
 * shell files in `#` lines. Detekt only reads Kotlin sources; this check also covers build scripts,
 * module descriptors and the configuration of the build.
 *
 * @param factory The object factory used to create the properties.
 * @since 4.3.0
 */
public abstract class LicenseHeaderExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * Whether `kreateLicenseHeaderCheck` is registered. Defaults to `false`.
     *
     * @since 4.3.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The notice without comment markers, one line per line of the header. Required once the check
     * is enabled; [apacheLicense] sets the notice of the Apache License 2.0.
     *
     * @since 4.3.0
     */
    public val notice: Property<String> = factory.property(String::class.java)

    /**
     * The Ant patterns, relative to the project directory, of the files that are checked. Defaults
     * to Kotlin, Kotlin script, Java, property and YAML files.
     *
     * @since 4.3.0
     */
    public val includes: SetProperty<String> = factory.setProperty(String::class.java).convention(DEFAULT_INCLUDES)

    /**
     * Further Ant patterns of files that are never checked. Empty by default. The output of Gradle,
     * Kotlin and the IDE, the Git directory and the files Gradle generates in `gradle/` are never
     * checked either, whatever this property holds.
     *
     * @since 4.3.0
     */
    public val excludes: SetProperty<String> = factory.setProperty(String::class.java).convention(emptySet())

    /**
     * Whether `check` runs `kreateLicenseHeaderCheck`. Defaults to `true`.
     *
     * @since 4.3.0
     */
    public val runOnCheck: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * Sets [notice] to the notice of the Apache License 2.0, headed by [copyright].
     *
     * @param copyright The copyright line, such as `Copyright 2026 Davils`.
     * @throws IllegalArgumentException If [copyright] is blank or spans more than one line.
     * @since 4.3.0
     */
    public fun apacheLicense(copyright: String) {
        notice.set(apacheLicenseNotice(copyright))
    }

    private companion object {
        val DEFAULT_INCLUDES: Set<String> = setOf(
            "**/*.kt",
            "**/*.kts",
            "**/*.java",
            "**/*.properties",
            "**/*.yaml",
            "**/*.yml",
        )
    }
}
