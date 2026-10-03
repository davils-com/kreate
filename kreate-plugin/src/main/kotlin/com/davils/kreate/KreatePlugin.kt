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

import com.davils.kreate.feature.KreateFeatures
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

/**
 * The Kreate project plugin, applied as `com.davils.kreate`.
 *
 * Registers the `kreate { }` extension and applies every Kreate feature once the build script has
 * been evaluated, so that each feature reads the configuration the consumer declared.
 *
 * @since 4.0.0
 */
public class KreatePlugin : Plugin<Project> {
    /**
     * Registers the `kreate` extension and schedules the features.
     *
     * @param project The project the plugin is applied to.
     * @since 4.0.0
     */
    override fun apply(project: Project) {
        val extension = project.extensions.create<KreateExtension>(EXTENSION_NAME)
        project.afterEvaluate { KreateFeatures.applyAll(this, extension) }
    }

    private companion object {
        private const val EXTENSION_NAME: String = "kreate"
    }
}
