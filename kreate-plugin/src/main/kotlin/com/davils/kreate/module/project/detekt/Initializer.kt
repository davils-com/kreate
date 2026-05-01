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

package com.davils.kreate.module.project.detekt

import com.davils.kreate.KreateExtension
import com.davils.kreate.module.project.detekt.extension.DetektExtension
import dev.detekt.gradle.Detekt
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import dev.detekt.gradle.extensions.DetektExtension as KDetektExtension

/**
 * Initializes the Detekt static analysis for the project.
 *
 * This function applies the Detekt plugin and configures its extension and tasks
 * if Detekt is enabled in the Kreate configuration.
 *
 * @param extension The main Kreate extension.
 * @since 1.2.0
 */
internal fun Project.initializeDetekt(extension: KreateExtension) {
    val detektExtension = extension.project.detekt
    if (!detektExtension.enabled.get()) {
         return
    }

    applyDetektPlugin()
    configureDetektExtension(detektExtension)
    configureDetektTasks(detektExtension)
}

private fun Project.configureDetektExtension(extension: DetektExtension) {
    extensions.configure<KDetektExtension> {
        config.setFrom(files(extension.config))
        buildUponDefaultConfig.set(extension.buildUponDefaultConfig)
        allRules.set(extension.allRules)
    }
}

private fun Project.configureDetektTasks(extension: DetektExtension) {
    tasks.withType<Detekt>().configureEach {
        reports {
            checkstyle {
                required.set(extension.reports.checkstyle.required)
                outputLocation.set(extension.reports.checkstyle.outputLocation)
            }

            html {
                required.set(extension.reports.html.required)
                outputLocation.set(extension.reports.html.outputLocation)
            }

            markdown {
                required.set(extension.reports.markdown.required)
                outputLocation.set(extension.reports.markdown.outputLocation)
            }

            sarif {
                required.set(extension.reports.sarif.required)
                outputLocation.set(extension.reports.sarif.outputLocation)
            }
        }
    }
}