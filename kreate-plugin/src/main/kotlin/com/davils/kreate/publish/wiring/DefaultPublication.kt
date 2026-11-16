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

package com.davils.kreate.publish.wiring

import com.davils.kreate.gradle.GradlePluginId
import com.davils.kreate.gradle.configurePom
import com.davils.kreate.publish.PublishExtension
import org.gradle.api.Project
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.withType

internal const val PUBLISHING_EXTENSION_NAME: String = "publishing"

private const val DEFAULT_PUBLICATION_NAME: String = "maven"

private const val JAVA_EXTENSION_NAME: String = "java"

private const val JAVA_COMPONENT_NAME: String = "java"

private const val JAVA_PLATFORM_COMPONENT_NAME: String = "javaPlatform"

internal fun configurePublicationPoms(
    publishing: PublishingExtension,
    publishConfig: PublishExtension,
    projectName: String,
    projectDescription: String?
) {
    publishing.publications.withType<MavenPublication>().configureEach {
        pom { configurePom(publishConfig, projectName, projectDescription) }
    }
}

internal fun Project.registerDefaultPublication(
    publishing: PublishingExtension,
    artifactId: String,
    target: String,
) {
    if (pluginManager.hasPlugin(GradlePluginId.MAVEN_CENTRAL)) return
    if (!publishing.publications.withType<MavenPublication>().isEmpty()) return

    val component = publishableComponent()
    if (component == null) {
        logger.warn(
            "Kreate's $target publishing is enabled for project '$path', but the project " +
                "publishes no software component ('java' or 'javaPlatform'). Apply a plugin " +
                "that provides one, or declare a publication yourself - `publish` will " +
                "otherwise succeed without uploading anything."
        )
        return
    }

    includeSourcesJar()

    publishing.publications.register(DEFAULT_PUBLICATION_NAME, MavenPublication::class.java) {
        from(component)
        this.artifactId = artifactId
    }
}

private fun Project.publishableComponent(): SoftwareComponent? =
    components.findByName(JAVA_COMPONENT_NAME) ?: components.findByName(JAVA_PLATFORM_COMPONENT_NAME)

private fun Project.includeSourcesJar() {
    if (!plugins.hasPlugin(GradlePluginId.JAVA)) return

    extensions.configure<JavaPluginExtension>(JAVA_EXTENSION_NAME) {
        withSourcesJar()
    }
}
