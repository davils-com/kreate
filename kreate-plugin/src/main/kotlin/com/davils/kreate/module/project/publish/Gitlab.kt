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

import com.davils.kreate.module.project.publish.extension.PublishExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.authentication.http.HttpHeaderAuthentication
import org.gradle.kotlin.dsl.*
import java.net.URI

internal fun Project.configureGitlab(
    publishConfig: PublishExtension,
    projectName: String,
    projectDescription: String?
) {
    val gitlabConfig = publishConfig.repositories.gitlab
    if (!gitlabConfig.enabled.get()) return

    val tokenEnvName = gitlabConfig.tokenEnv.get()
    val jobToken = System.getenv(tokenEnvName)

    if (jobToken == null) {
        logger.lifecycle("No CI job token found in $tokenEnvName, skipping GitLab publish repository")
        return
    }

    val repoName = gitlabConfig.name.orNull ?: "GitlabPackageRegistry"
    val projectId = System.getenv(gitlabConfig.projectIdEnv.get())
    val apiV4 = System.getenv(gitlabConfig.apiUrlEnv.get())

    pluginManager.apply(MavenPublishPlugin::class.java)

    extensions.configure<PublishingExtension>("publishing") {
        repositories {
            maven {
                name = repoName
                url = URI("$apiV4/projects/$projectId/packages/maven")

                credentials(PasswordCredentials::class) {
                    username = "Job-Token"
                    password = jobToken
                }

                authentication {
                    create<HttpHeaderAuthentication>("token")
                }
            }
        }

        publications.withType<MavenPublication>().configureEach {
            pom {
                configurePom(publishConfig, projectName, projectDescription)
            }
        }
    }
}

