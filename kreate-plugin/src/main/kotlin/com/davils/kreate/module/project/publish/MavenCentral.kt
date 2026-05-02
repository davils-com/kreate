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

package com.davils.kreate.module.project.publish

import com.davils.kreate.KreateExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.MavenPublishBasePlugin
import org.gradle.api.Project

/**
 * Configures publishing to Maven Central using the vanniktech plugin.
 *
 * This function sets up coordinates, signing, and automatic release settings
 * and configures the Maven POM with project metadata.
 *
 * @param kreateExtension The Kreate configuration extension.
 * @since 1.0.0
 */
internal fun Project.configureMavenCentral(
    kreateExtension: KreateExtension,
) {
    val publishConfig = kreateExtension.project.publish
    val mavenCentralConfig = publishConfig.repositories.mavenCentral
    if (!mavenCentralConfig.enabled.get()) {
        return
    }

    val projectName = kreateExtension.project.name.orNull ?: project.name
    val projectDescription = kreateExtension.project.description.orNull
    val projectGroup = project.group.toString()

    if (!plugins.hasPlugin(MavenPublishBasePlugin::class.java)) {
        error("Maven Publish Base Plugin not applied. Do it yourself: 'com.vanniktech.maven.publish'")
    }

    extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
        publishToMavenCentral(automaticRelease = mavenCentralConfig.automaticRelease.get())
        if (mavenCentralConfig.signPublications.get()) {
            signAllPublications()
        }

        coordinates(projectGroup, projectName, version.toString())

        pom {
            configurePom(publishConfig, projectName, projectDescription)
        }
    }
}
