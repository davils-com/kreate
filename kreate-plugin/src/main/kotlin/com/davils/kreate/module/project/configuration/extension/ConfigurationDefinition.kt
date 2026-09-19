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

package com.davils.kreate.module.project.configuration.extension

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * One configuration schema the build knows how to reach.
 *
 * A schema is built by Kotlin code rather than written down as data, so a build cannot find one by
 * looking: it has to be told which declaration to read and where that declaration lives. The two
 * names below are that address, and they are the whole extension point.
 *
 * ```kotlin
 * schema("server") {
 *     holder = "com.acme.config.ServerConfigKt"
 *     accessor = "getServerSchema"
 * }
 * ```
 *
 * The holder is the JVM class the declaration compiled into - for a top level `val` in
 * `ServerConfig.kt` that is `…ServerConfigKt`, and for an `object` it is the object's own class. The
 * accessor is a method on it that takes no arguments; `serverSchema` is accepted as well as
 * `getServerSchema`, because which of the two a property compiles to is not something a build file
 * should have to know.
 *
 * @since 3.1.0
 */
public abstract class ConfigurationDefinition {
    /**
     * What this definition is called in the build, and the file name its export is written to.
     *
     * @since 3.1.0
     */
    @get:Input
    public abstract val name: Property<String>

    /**
     * The fully qualified JVM class the declaration is reached through.
     *
     * @since 3.1.0
     */
    @get:Input
    public abstract val holder: Property<String>

    /**
     * The no-argument method on [holder] that hands the declaration back.
     *
     * Optional, and it defaults to [name]: a definition called `server` is looked up as
     * `getServer` and then as `server`, which is what a top level `val server` compiles to.
     *
     * @since 3.1.0
     */
    @get:Input
    @get:Optional
    public abstract val accessor: Property<String>
}
