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

private val DEFAULT_INCLUDES: Set<String> = setOf("src/**", "*.md", "docs/**")

/**
 * Configuration of the search of a project's files for standards it names but does not declare.
 *
 * @param factory The object factory used to create the properties.
 * @since 5.3.0
 */
public abstract class AssuranceStandardsInventoryExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * Whether the files are searched for references such as `RFC 7396` or a link to
     * `w3.org/TR/`. Defaults to `true`.
     *
     * @since 5.3.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * The Ant patterns, relative to the project directory, of the files that are searched.
     * Defaults to every file below `src` and `docs` and the Markdown files of the project
     * directory.
     *
     * @since 5.3.0
     */
    public val includes: SetProperty<String> = factory.setProperty(String::class.java).convention(DEFAULT_INCLUDES)

    /**
     * Further Ant patterns of files that are never searched. Empty by default. The output of
     * Gradle, Kotlin and the IDE and the lock file of the standards are never searched either.
     *
     * @since 5.3.0
     */
    public val excludes: SetProperty<String> = factory.setProperty(String::class.java).convention(emptySet())
}
