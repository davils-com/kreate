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

import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configuration of the baseline, the accepted findings of an existing code base.
 *
 * @param factory The object factory used to create the properties.
 * @param layout The layout of the project, which locates the default file.
 * @since 4.0.0
 */
public abstract class AssuranceBaselineExtension @Inject constructor(factory: ObjectFactory, layout: ProjectLayout) {
    /**
     * The baseline file. Defaults to `kreate-baseline.json` in the project directory.
     *
     * @since 4.0.0
     */
    public val file: RegularFileProperty = factory.fileProperty().convention(
        layout.projectDirectory.file("kreate-baseline.json")
    )

    /**
     * The most severe finding the baseline may accept. When absent, it accepts every severity.
     *
     * @since 4.0.0
     */
    public val maxSeverity: Property<Severity> = factory.property(Severity::class.java)

    /**
     * Whether an entry that no longer matches anything fails the build until it is pruned.
     *
     * @since 4.0.0
     */
    public val failOnStale: Property<Boolean> = factory.property(Boolean::class.java).convention(false)
}
