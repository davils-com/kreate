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

package com.davils.kreate.module.builder

import com.davils.kreate.KreateDsl
import com.davils.kreate.KreateExtension
import com.davils.kreate.module.Module
import org.gradle.api.Project

/**
 * Builder for creating a [KreateModuleRegistryData].
 *
 * This builder provides a DSL for registering [Module]s to be applied to a project.
 *
 * @param project The Gradle project to which the modules will be applied.
 * @param extension The Kreate extension containing the configuration.
 * @since 1.0.0
 */
@KreateDsl
internal class KreateModuleRegistryBuilder(
    /**
     * The project instance.
     * @since 1.0.0
     */
    private val project: Project,
    /**
     * The extension instance.
     * @since 1.0.0
     */
    private val extension: KreateExtension
) {
    private val modules = mutableListOf<Module>()

    /**
     * Adds a module to the registry if it hasn't been added yet.
     *
     * @param module The module to add.
     * @since 1.0.0
     */
    fun add(module: Module) {
        if (modules.contains(module)) return
        modules.add(module)
    }

    /**
     * Adds a collection of modules to the registry.
     *
     * @param modules The collection of modules to add.
     * @since 1.0.0
     */
    fun addAll(modules: Iterable<Module>) {
        this.modules.addAll(modules)
    }

    /**
     * Adds multiple modules to the registry.
     *
     * @param modules The modules to add.
     * @since 1.0.0
     */
    fun addAll(vararg modules: Module) {
        addAll(modules.asList())
    }

    /**
     * Operator function to add a module via the unary plus operator.
     *
     * @since 1.0.0
     */
    operator fun Module.unaryPlus() {
        add(this)
    }

    /**
     * Builds and returns a [KreateModuleRegistryData] instance.
     *
     * @return A new instance of [KreateModuleRegistryData].
     * @since 1.0.0
     */
    fun build(): KreateModuleRegistryData {
        return KreateModuleRegistryData(
            extension = extension,
            project = project,
            modules = modules.toList()
        )
    }
}
