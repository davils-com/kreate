package com.davils.kreate.module.project.docs

import com.davils.kreate.Davils
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.plugins.DokkaHtmlPluginParameters
import java.time.Year

internal fun Project.configureDokkaExtension(docsExtension: DocsExtension) {
    extensions.configure<DokkaExtension> {
        if (docsExtension.moduleName.isPresent) {
            moduleName.set(docsExtension.moduleName.get())
        }

        if (docsExtension.outputDirectory.isPresent) {
            dokkaPublications.named("html") {
                outputDirectory.set(layout.buildDirectory.dir(docsExtension.outputDirectory.get()))
            }
        }

        pluginsConfiguration.named("html", DokkaHtmlPluginParameters::class) {
            footerMessage.set("Copyright (c) 2024 - ${Year.now()} ${Davils.Organization.NAME}")
        }
    }
}