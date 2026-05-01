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

import com.davils.kreate.KreateExtension
import com.davils.kreate.module.Module
import org.gradle.api.Project

/**
 * Registry for Kreate modules.
 *
 * This class manages a set of [Module]s and provides functionality to apply
 * them to the associated Gradle project.
 *
 * @param data The data containing the project, extension, and modules.
 * @since 1.0.0
 */
internal class KreateModuleRegistry(
    /**
     * The registry data.
     * @since 1.0.0
     */
    private val data: KreateModuleRegistryData
) {
    private var isApplied = false

    /**
     * Retrieves a copy of the registered modules.
     *
     * @return A list of [Module]s.
     * @since 1.0.0
     */
    fun getModules(): List<Module> = data.modules.toList()

    /**
     * Applies all registered modules to the project.
     *
     * This method ensures that modules are only applied once.
     * @since 1.0.0
     */
    fun applyAll() {
        if (isApplied) {
            return
        }

        data.modules.forEach { module ->
            module.apply(data.project, data.extension)
        }
        isApplied = true
    }
}

/**
 * Creates and configures a [KreateModuleRegistry] for the project.
 *
 * @param extension The Kreate configuration extension.
 * @param builder The DSL block for registering modules.
 * @return A configured [KreateModuleRegistry].
 * @since 1.0.0
 */
internal fun Project.modules(
    extension: KreateExtension,
    builder: KreateModuleRegistryBuilder.() -> Unit
): KreateModuleRegistry {
    val registryBuilder = KreateModuleRegistryBuilder(this, extension)
    registryBuilder.builder()
    val registryData = registryBuilder.build()
    return KreateModuleRegistry(registryData)
}
