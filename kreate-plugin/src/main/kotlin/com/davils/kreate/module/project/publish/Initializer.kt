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
import com.davils.kreate.module.local.gatherLocalModeInputs
import com.davils.kreate.module.local.isActive
import com.davils.kreate.module.local.resolveLocalMode
import com.davils.kreate.module.local.workspace
import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.withType

/**
 * The id of Gradle's own Maven publishing plugin, which owns the `publishing { }` extension.
 *
 * @since 3.0.0
 */
internal const val MAVEN_PUBLISH_PLUGIN_ID: String = "maven-publish"

/**
 * The id of the Maven Central publishing plugin Kreate applies.
 *
 * @since 3.0.0
 */
internal const val MAVEN_CENTRAL_PLUGIN_ID: String = "com.vanniktech.maven.publish"

/**
 * Initializes publishing configuration for the project.
 *
 * If publishing is enabled in the extension, this function configures
 * Maven Central and/or GitLab repositories for deployment.
 *
 * @param extension The Kreate configuration extension.
 * @since 1.0.0
 */
internal fun Project.initializePublish(extension: KreateExtension) {
    val publishConfig = extension.project.publish
    if (!publishConfig.enabled.get()) return

    configureMavenCentral(extension)
    configureGitlab(extension)
    guardRemotePublishing(extension)
}

/**
 * Stops a build that resolves local publications from pushing its output to a shared registry.
 *
 * An artefact built in local mode was compiled against dependencies that exist on one machine.
 * Publishing it to GitLab or Maven Central would produce a release nobody can reproduce and whose
 * own dependencies cannot be resolved — and it would look entirely normal doing so, because every
 * version in the published POM is one that also exists upstream.
 *
 * Only [PublishToMavenRepository] is matched, and that is sufficient rather than merely
 * convenient: it and [PublishToMavenLocal] are siblings under `AbstractPublishToMaven`, not one a
 * subclass of the other, so `publishToMavenLocal` — the publication local mode exists to produce
 * — is already outside the selection. Widening this to `AbstractPublishToMaven` would break the
 * feature it is meant to protect.
 *
 * @param extension The main Kreate extension.
 * @since 3.2.0
 */
private fun Project.guardRemotePublishing(extension: KreateExtension) {
    val mode = resolveLocalMode(
        gatherLocalModeInputs(
            providers,
            gradle.gradleUserHomeDir,
            extension.local.ciEnvironmentVariables.get()
        )
    )
    if (!mode.isActive) return

    val substituted = mode.workspace.libraries.joinToString { "${it.library}:${it.version}" }

    tasks.withType<PublishToMavenRepository>()
        .configureEach {
            doFirst {
                error(
                    """
                        Refusing to publish to a remote repository while resolving from the local
                        Maven repository.

                        This build resolves: $substituted

                        Those artefacts exist on this machine only. Anything published against
                        them could not be resolved by anyone else, and the published metadata
                        would not say so.

                        Clear the local publications first:

                            ./gradlew ${com.davils.kreate.KreateTasks.Local.CLEAN}
                    """.trimIndent()
                )
            }
        }
}
