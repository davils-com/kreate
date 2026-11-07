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

/**
 * Configuration of the aggregation, which puts the gates of all projects of a build into one report.
 *
 * Every project that should be aggregated enables it; the root project collects the results. With the
 * settings plugin `com.davils.kreate.settings`, the root project knows all projects of the build.
 *
 * @param factory The object factory used to create the properties.
 * @since 4.0.0
 */
public abstract class AssuranceAggregateExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * Whether the results of this project are shared, and collected in the root project. Defaults to `false`.
     *
     * @since 4.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The paths of the projects the root project collects. Defaults to every project of the build when
     * the settings plugin is applied, and to the root project otherwise.
     *
     * @since 4.0.0
     */
    public val members: SetProperty<String> = factory.setProperty(String::class.java)
}
