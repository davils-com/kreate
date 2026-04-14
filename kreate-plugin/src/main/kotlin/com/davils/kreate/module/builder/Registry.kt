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

internal class KreateModuleRegistry(private val data: KreateModuleRegistryData) {
    private var isApplied = false

    fun getModules(): List<Module> {
        return data.modules.toList()
    }

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

internal fun Project.modules(extension: KreateExtension, builder: KreateModuleRegistryBuilder.() -> Unit): KreateModuleRegistry {
    val registryBuilder = KreateModuleRegistryBuilder(this, extension)
    registryBuilder.builder()
    val registryData = registryBuilder.build()
    return KreateModuleRegistry(registryData)
}