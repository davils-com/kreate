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
 * Data class holding the registry of Kreate modules for a project.
 *
 * It contains the configuration extension, the project instance, and the list
 * of registered modules to be applied.
 *
 * @since 1.0.0
 */
internal data class KreateModuleRegistryData(
    /**
     * The Kreate configuration extension.
     * @since 1.0.0
     */
    val extension: KreateExtension,
    /**
     * The Gradle project instance.
     * @since 1.0.0
     */
    val project: Project,
    /**
     * The list of registered modules.
     * @since 1.0.0
     */
    val modules: List<Module>
)