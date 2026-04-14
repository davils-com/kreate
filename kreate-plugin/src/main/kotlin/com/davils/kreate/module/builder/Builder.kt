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

@KreateDsl
internal class KreateModuleRegistryBuilder(
    private val project: Project,
    private val extension: KreateExtension
) {
    private val modules = mutableListOf<Module>()

    fun add(module: Module) {
        if (modules.contains(module)) return
        modules.add(module)
    }

    fun addAll(modules: Iterable<Module>) {
        this.modules.addAll(modules)
    }

    fun addAll(vararg modules: Module) {
        addAll(modules.asList())
    }

    operator fun Module.unaryPlus() {
        add(this)
    }

    fun build(): KreateModuleRegistryData {
        return KreateModuleRegistryData(
            extension = extension,
            project = project,
            modules = modules.toList()
        )
    }
}