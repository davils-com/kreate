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

package com.davils.kreate

import com.davils.kreate.module.builder.modules
import com.davils.kreate.module.platform.PlatformModule
import com.davils.kreate.module.project.ProjectModule
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

/**
 * The main plugin class for Kreate.
 *
 * This plugin sets up the "kreate" extension and applies necessary modules
 * to the project.
 *
 * @since 1.0.0
 */
public class Kreate : Plugin<Project> {
    /**
     * Applies the plugin to the given project.
     *
     * @param project The project to which the plugin is applied.
     * @since 1.0.0
     */
    override fun apply(project: Project) {
        val kreateExtension = project.extensions.create<KreateExtension>("kreate")
        project.addModules(kreateExtension)
    }
}

/**
 * Adds and applies modules to the project based on the extension configuration.
 *
 * @param extension The [KreateExtension] used for configuration.
 * @since 1.0.0
 */
private fun Project.addModules(extension: KreateExtension) {
    val modules = modules(extension) {
        add(ProjectModule)
        add(PlatformModule)
    }
    modules.applyAll()
}