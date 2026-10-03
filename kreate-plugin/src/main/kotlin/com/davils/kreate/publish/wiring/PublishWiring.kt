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

import com.davils.kreate.KreateExtension
import com.davils.kreate.local.LocalWorkflowTaskNames
import com.davils.kreate.settings.local.gatherLocalModeInputs
import com.davils.kreate.settings.local.isActive
import com.davils.kreate.settings.local.resolveLocalMode
import com.davils.kreate.settings.local.workspace
import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.withType

internal fun Project.initializePublish(extension: KreateExtension) {
    val publishConfig = extension.project.publish
    if (!publishConfig.enabled.get()) return

    configureMavenCentral(extension)
    configureGitLab(extension)
    guardRemotePublishing(extension)
}

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

                            ./gradlew ${LocalWorkflowTaskNames.CLEAN}
                    """.trimIndent()
                )
            }
        }
}
