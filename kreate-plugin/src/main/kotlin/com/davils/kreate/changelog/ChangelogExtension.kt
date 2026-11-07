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
package com.davils.kreate.changelog

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configuration of the changelog: its checks, the section generated from conventional commits and the
 * release notes taken from it.
 *
 * @param factory The object factory used to create the properties.
 * @param project The project the changelog belongs to.
 * @since 4.0.0
 */
public abstract class ChangelogExtension @Inject constructor(factory: ObjectFactory, project: Project) {
    /**
     * Whether the changelog tasks are registered. Defaults to `false`.
     *
     * @since 4.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The changelog. Defaults to `CHANGELOG.md` in the root project.
     *
     * @since 4.0.0
     */
    public val file: RegularFileProperty = factory.fileProperty().convention(
        project.isolated.rootProject.projectDirectory.file("CHANGELOG.md")
    )

    /**
     * The prefix of release tags, such as `v`. Defaults to none.
     *
     * @since 4.0.0
     */
    public val tagPrefix: Property<String> = factory.property(String::class.java).convention("")

    /**
     * Whether `check` runs `kreateChangelogCheck`. Defaults to `true`.
     *
     * @since 4.0.0
     */
    public val runOnCheck: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * Whether a generated section is headed with the date of the release. Defaults to `true`.
     *
     * @since 4.0.0
     */
    public val includeDates: Property<Boolean> = factory.property(Boolean::class.java).convention(true)
}
