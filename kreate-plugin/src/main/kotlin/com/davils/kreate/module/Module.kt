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

package com.davils.kreate.module

import com.davils.kreate.KreateExtension
import org.gradle.api.Project

/**
 * Represents a module that can be applied to a project.
 *
 * Modules encapsulate specific functionality or configurations that can be
 * added to a Gradle project through the Kreate plugin.
 *
 * @since 1.0.0
 */
internal interface Module {
    /**
     * Applies the module configuration to the project.
     *
     * @param project The Gradle project to configure.
     * @param extension The Kreate extension containing the configuration.
     * @since 1.0.0
     */
    fun apply(project: Project, extension: KreateExtension)
}
