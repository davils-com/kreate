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

private const val DEFAULT_PUBLIC_REPOSITORY: String = "https://repo.maven.apache.org/maven2"

/**
 * Configuration of the confusion checks: typosquatting, inflated versions, plugin markers that
 * lead outside the plugin's namespace and, on request, internal modules that also exist publicly.
 *
 * @param factory The object factory used to create the properties.
 * @since 4.0.0
 */
public abstract class AssuranceConfusionExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * Whether the checks run. Defaults to `false`.
     *
     * @since 4.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * Further modules, as `group:artifact`, that typosquatting would imitate, in addition to the
     * built-in list.
     *
     * @since 4.0.0
     */
    public val popularModules: SetProperty<String> = factory.setProperty(String::class.java)

    /**
     * Groups, `group:artifact` coordinates or plugin ids that are never reported.
     *
     * @since 4.0.0
     */
    public val allow: SetProperty<String> = factory.setProperty(String::class.java)

    /**
     * Whether the modules of `internalGroups` are looked up in [publicRepository]. This sends
     * their coordinates to that repository. Defaults to `false`.
     *
     * @since 4.0.0
     */
    public val checkPublicRepository: Property<Boolean> =
        factory.property(Boolean::class.java).convention(false)

    /**
     * The public Maven repository that internal modules are looked up in. Defaults to Maven Central.
     *
     * @since 4.0.0
     */
    public val publicRepository: Property<String> =
        factory.property(String::class.java).convention(DEFAULT_PUBLIC_REPOSITORY)

    /**
     * Whether the plugins on the build script class path are checked as well. Defaults to `true`.
     *
     * @since 4.0.0
     */
    public val includeBuildscript: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * The configurations to check. Defaults to the classpaths of the main code.
     *
     * @since 4.0.0
     */
    public val configurations: SetProperty<String> = factory.setProperty(String::class.java)
}
