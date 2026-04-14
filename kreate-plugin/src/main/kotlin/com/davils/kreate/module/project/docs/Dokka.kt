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

package com.davils.kreate.module.project.docs

import com.davils.kreate.KreateExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.plugins.DokkaHtmlPluginParameters

internal fun Project.configureDokkaExtension(kreateExtension: KreateExtension) {
    val docsExtension = kreateExtension.project.docs

    extensions.configure<DokkaExtension> {
        if (docsExtension.moduleName.isPresent) {
            moduleName.set(docsExtension.moduleName.get())
        }

        if (docsExtension.outputDirectory.isPresent) {
            dokkaPublications.named("html") {
                outputDirectory.set(layout.buildDirectory.dir(docsExtension.outputDirectory.get()))
            }
        }

        if (!docsExtension.copyright.isPresent) {
            return@configure
        }

        pluginsConfiguration.named("html", DokkaHtmlPluginParameters::class) {
            footerMessage.set(docsExtension.copyright.get())
        }
    }
}