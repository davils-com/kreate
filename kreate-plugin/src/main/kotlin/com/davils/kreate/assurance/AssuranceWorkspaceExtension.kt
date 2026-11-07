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
 * Configuration of the workspace check, which looks at the projects of a build together: modules
 * resolved in different versions, cycles between projects and dependencies the layers forbid.
 *
 * Every project that should be checked applies Kreate with the workspace check enabled; the root
 * project collects them. With the settings plugin `com.davils.kreate.settings` applied, the root
 * project knows all projects of the build without further configuration.
 *
 * @param factory The object factory used to create the properties.
 * @since 4.0.0
 */
public abstract class AssuranceWorkspaceExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * Whether the workspace check runs. Defaults to `false`.
     *
     * @since 4.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The paths of the projects the root project collects. Defaults to every project of the build
     * when the settings plugin is applied, and to the root project otherwise.
     *
     * @since 4.0.0
     */
    public val members: SetProperty<String> = factory.setProperty(String::class.java)

    /**
     * The configurations whose dependencies a project shares with the workspace. Defaults to the
     * classpaths of the main code.
     *
     * @since 4.0.0
     */
    public val configurations: SetProperty<String> = factory.setProperty(String::class.java)

    /**
     * Whether a module resolved in different versions by different projects is reported. Defaults to `true`.
     *
     * @since 4.0.0
     */
    public val alignVersions: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * The revision `kreateWorkspaceAffected` compares with. Defaults to `origin/main`.
     *
     * @since 4.0.0
     */
    public val affectedBase: Property<String> = factory.property(String::class.java).convention("origin/main")

    /**
     * The forbidden dependencies, as `from -> to`, written through [forbid].
     *
     * @since 4.0.0
     */
    public val forbidden: SetProperty<String> = factory.setProperty(String::class.java)

    /**
     * Forbids projects matching [from] to depend on projects matching [to]. `*` matches within one
     * path segment and `**` across segments, so `:core:**` names everything below `:core`.
     *
     * @param from The pattern of the depending projects.
     * @param to The pattern of the projects they must not depend on.
     * @since 4.0.0
     */
    public fun forbid(from: String, to: String) {
        forbidden.add("$from -> $to")
    }
}
