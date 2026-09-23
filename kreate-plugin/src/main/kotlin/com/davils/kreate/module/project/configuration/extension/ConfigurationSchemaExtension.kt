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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Extension for configuring configuration schema export and compatibility checking.
 *
 * A configuration schema is a promise to every document already written against it, in exactly the
 * way a published signature is a promise to everything compiled against one. `kreateApiCheck` refuses
 * a signature change that is not recorded; this refuses a schema change that would stop a deployed
 * file loading, and it is deliberately the same shape: a checked-in export, a task that writes it and
 * a task that compares against it, wired into `check`.
 *
 * ```kotlin
 * kreate {
 *     project {
 *         configurationSchema {
 *             enabled = true
 *             schema("server") { holder = "com.acme.config.ServerConfigKt" }
 *             validation { holder = "com.acme.config.ValidationKt"; accessor = "configurationReports" }
 *         }
 *     }
 * }
 * ```
 *
 * **Kreate does not depend on the configuration library.** Everything is reached by reflection over
 * the project's own runtime classpath, so a project that does not use one is unaffected and a project
 * that does is not pinned to whichever version this plugin was built against.
 *
 * @since 3.1.0
 */
public abstract class ConfigurationSchemaExtension @Inject constructor(
    private val factory: ObjectFactory,
    project: Project
) {
    /**
     * Whether schema export and compatibility checking are enabled for this project.
     *
     * Defaults to `false`, in line with every other Kreate feature.
     *
     * @since 3.1.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The directory holding the checked-in exports.
     *
     * Defaults to `config-schema` below the project directory, beside `api` and read the same way: it
     * is a reviewed artefact rather than build output, so it belongs in the source tree and under the
     * same CODEOWNERS rule that guards the `.api` dump.
     *
     * @since 3.1.0
     */
    public val schemaDirectory: DirectoryProperty = factory.directoryProperty().convention(
        project.layout.projectDirectory.dir("config-schema")
    )

    /**
     * The schemas this project exports and checks.
     *
     * @since 3.1.0
     */
    @get:Nested
    public val definitions: ListProperty<ConfigurationDefinition> =
        factory.listProperty(ConfigurationDefinition::class.java)

    /**
     * Where the reports of a dry run over this repository's own configuration files come from, or
     * nothing to run none.
     *
     * @since 3.1.0
     */
    @get:Nested
    public val validation: Property<ConfigurationDefinition> =
        factory.property(ConfigurationDefinition::class.java)

    /**
     * Declares one schema to export and check.
     *
     * @param name What it is called, which is also the file name its export is written to.
     * @param action Configures where the declaration is reached.
     * @since 3.1.0
     */
    public fun schema(name: String, action: Action<ConfigurationDefinition>) {
        val definition = factory.newInstance(ConfigurationDefinition::class.java)
        definition.name.set(name)
        action.execute(definition)
        definitions.add(definition)
    }

    /**
     * Declares where the dry run over this repository's own configuration files is reached.
     *
     * The accessor takes no arguments and hands back the list of reports a validation produced -
     * which, because the check has to resolve the sources and the secret resolvers a real load would
     * resolve, is a few lines of the project's own code rather than something a build can assemble.
     * A build that ran the library's own DSL for it would be a second place the declaration is
     * written, and the two would disagree the first time either moved.
     *
     * @param action Configures where the reports are reached.
     * @since 3.1.0
     */
    public fun validation(action: Action<ConfigurationDefinition>) {
        val definition = factory.newInstance(ConfigurationDefinition::class.java)
        definition.name.set("validation")
        action.execute(definition)
        validation.set(definition)
    }
}
