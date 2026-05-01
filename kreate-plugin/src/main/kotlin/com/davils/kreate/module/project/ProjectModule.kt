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

package com.davils.kreate.module.project

import com.davils.kreate.KreateExtension
import com.davils.kreate.module.Module
import com.davils.kreate.module.project.constants.initializeBuildConstants
import com.davils.kreate.module.project.docs.initializeDocs
import com.davils.kreate.module.project.publish.initializePublish
import com.davils.kreate.module.project.tests.initializeTesting
import org.gradle.api.Project

/**
 * Module for configuring project-level settings.
 *
 * This module applies default Gradle plugins, adds repositories, and
 * initializes various project components like versioning, documentation,
 * testing, and publishing.
 *
 * @since 1.0.0
 */
internal object ProjectModule : Module {
    /**
     * Applies the project module configuration to the project.
     *
     * @param project The Gradle project to configure.
     * @param extension The Kreate configuration extension.
     * @since 1.0.0
     */
    override fun apply(project: Project, extension: KreateExtension) {
        project.configureCommon(extension)
    }

    private fun Project.configureCommon(extension: KreateExtension) {
        applyDefaultGradlePlugins()
        addRepositories()

        afterEvaluate {
            configureVersion(extension.project.version.environment.get(), extension.project.version.property.get())
            initializeProject(extension.project)
            initializeBuildConstants(extension)
            initializeDocs(extension)
            initializeTesting(extension)
            initializePublish(extension)
        }
    }
}
