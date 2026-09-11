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

package com.davils.kreate.module.platform.jvm.jni

import com.davils.kreate.KreateExtension
import com.davils.kreate.module.project.publish.configurePom
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.register

/**
 * Registers one Maven publication per platform.
 *
 * Each platform becomes its own artifact id — `mylib-linux-x86_64` next to `mylib` — rather than a
 * classifier on the main one. The artifact is a pure resource carrier and declares no
 * dependencies: a dependency on the main library would point the wrong way, since it is the
 * consumer that pulls both.
 *
 * Nothing is registered when the consumer has not enabled any publishing target, so that
 * enabling native publishing alone does not quietly turn a project into a publishing one.
 *
 * @param extension The Kreate configuration extension.
 * @param projectName The base artifact name the platform suffix is appended to.
 * @param platforms The platforms selected for publishing.
 * @param jars The JAR task of each platform, in the same order.
 * @since 2.2.0
 */
internal fun Project.registerNativePublications(
    extension: KreateExtension,
    projectName: String,
    platforms: List<String>,
    jars: List<TaskProvider<Jar>>
) {
    val publishConfig = extension.project.publish
    if (!publishConfig.enabled.get()) return

    if (!plugins.hasPlugin("maven-publish")) {
        logger.warn(
            "Kreate's per-platform native publishing is enabled for project '$path', but the " +
                "'maven-publish' plugin is not applied. The platform JARs are built but nothing " +
                "publishes them."
        )
        return
    }

    val artifactBase = extension.project.name.orNull ?: projectName
    val projectDescription = extension.project.description.orNull
    val needsEmptyDocs = publishConfig.repositories.mavenCentral.enabled.get()

    // Maven Central rejects any artifact that ships without a sources and a javadoc JAR. A
    // resource-only artifact has neither to give, so it gets empty ones — the same thing every
    // project publishing native artifacts to Central ends up doing. The GitLab registry does not
    // ask, so the JARs are only produced when Central is a target.
    val emptyDocs = if (needsEmptyDocs) registerEmptyDocumentationJars(artifactBase) else emptyList()

    extensions.configure<PublishingExtension>("publishing") {
        // Kreate registers the main publication from the enabled repository targets, and it does
        // so before this runs. If none exists here, the release would consist of shared objects
        // and no library — and it would upload perfectly happily.
        if (publications.withType(MavenPublication::class.java).isEmpty()) {
            throw org.gradle.api.GradleException(
                """
                    Kreate's per-platform native publishing is enabled for project '$path', but
                    the project has no publication for the library itself.

                    Publishing only the platform artifacts would produce a release containing
                    native binaries and no code to call them.

                    Enable a publishing target so that the main artifact is published too:

                        kreate { project { publish { repositories {
                            gitlab { enabled = true }        // or
                            mavenCentral { enabled = true }
                        } } } }

                    Or declare a MavenPublication of your own.
                """.trimIndent()
            )
        }

        platforms.forEachIndexed { index, platformId ->
            publications.register("kreateNative${platformId.publicationSuffix()}", MavenPublication::class.java) {
                this.groupId = project.group.toString()
                this.artifactId = "$artifactBase-$platformId"
                this.version = project.version.toString()

                artifact(jars[index])
                emptyDocs.forEach { docJar -> artifact(docJar) }

                pom {
                    configurePom(
                        publishConfig,
                        "$artifactBase ($platformId)",
                        projectDescription?.let { "$it — native library for $platformId." }
                            ?: "Native library for $platformId."
                    )
                }
            }
        }
    }
}

/**
 * Registers the empty sources and javadoc JARs shared by every platform publication.
 *
 * @param artifactBase The base artifact name.
 * @return The registered JAR tasks.
 * @since 2.2.0
 */
private fun Project.registerEmptyDocumentationJars(artifactBase: String): List<TaskProvider<Jar>> {
    val sources = tasks.register<Jar>("kreateJniNativeSourcesJar") {
        group = com.davils.kreate.KreateTasks.Jni.GROUP
        description = "An empty sources JAR, required by Maven Central for native artifacts."
        archiveBaseName.set(artifactBase)
        archiveClassifier.set("sources")
        destinationDirectory.set(layout.buildDirectory.dir("libs/native-docs"))
    }

    val javadoc = tasks.register<Jar>("kreateJniNativeJavadocJar") {
        group = com.davils.kreate.KreateTasks.Jni.GROUP
        description = "An empty javadoc JAR, required by Maven Central for native artifacts."
        archiveBaseName.set(artifactBase)
        archiveClassifier.set("javadoc")
        destinationDirectory.set(layout.buildDirectory.dir("libs/native-docs"))
    }

    return listOf(sources, javadoc)
}

/**
 * Converts a platform identifier into the suffix of a publication name.
 *
 * @return The suffix, for example `LinuxX64`.
 * @since 2.2.0
 */
private fun String.publicationSuffix(): String = split('-')
    .joinToString(separator = "") { segment -> segment.replaceFirstChar { it.uppercaseChar() } }
