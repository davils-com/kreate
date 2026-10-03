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
import com.davils.kreate.gradle.GradlePluginId
import com.davils.kreate.gradle.configurePom
import com.davils.kreate.gradle.declaredProjectName
import com.davils.kreate.publish.GitLabExtension
import com.davils.kreate.publish.GitLabHeader
import com.davils.kreate.publish.PublishExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.authentication.http.HttpHeaderAuthentication
import org.gradle.kotlin.dsl.credentials
import org.gradle.kotlin.dsl.withType
import java.net.URI

private const val DEFAULT_GITLAB_REPOSITORY_NAME: String = "GitlabPackageRegistry"

private const val DEFAULT_PUBLICATION_NAME: String = "maven"

private const val PUBLISHING_EXTENSION_NAME: String = "publishing"

private const val JAVA_EXTENSION_NAME: String = "java"

private const val JAVA_COMPONENT_NAME: String = "java"

private const val JAVA_PLATFORM_COMPONENT_NAME: String = "javaPlatform"

private const val UNSET_DESCRIPTION: String = "unset"

internal fun Project.configureGitLab(
    kreateExtension: KreateExtension,
) {
    val publishConfig = kreateExtension.project.publish
    val gitLabConfig = publishConfig.repositories.gitlab
    if (!gitLabConfig.enabled.get()) return

    val projectName = declaredProjectName(kreateExtension)
    val projectDescription = kreateExtension.project.description.orNull

    extensions.configure<PublishingExtension>(PUBLISHING_EXTENSION_NAME) {
        registerDefaultPublication(this, projectName)
        addGitLabRepository(this, gitLabConfig)
        configurePublicationPoms(this, publishConfig, projectName, projectDescription)
    }
}

private fun configurePublicationPoms(
    publishing: PublishingExtension,
    publishConfig: PublishExtension,
    projectName: String,
    projectDescription: String?
) {
    publishing.publications.withType<MavenPublication>().configureEach {
        pom { configurePom(publishConfig, projectName, projectDescription) }
    }
}

private fun Project.registerDefaultPublication(
    publishing: PublishingExtension,
    artifactId: String,
) {
    if (!publishing.publications.withType<MavenPublication>().isEmpty()) return

    val component = publishableComponent()
    if (component == null) {
        logger.warn(
            "Kreate's GitLab publishing is enabled for project '$path', but the project " +
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

private fun Project.addGitLabRepository(
    publishing: PublishingExtension,
    gitLabConfig: GitLabExtension,
) {
    val tokenVariable = gitLabConfig.tokenEnv.get()
    val jobToken = environmentValueOf(tokenVariable)
    if (jobToken == null) {
        logger.lifecycle("No CI job token found in $tokenVariable, skipping GitLab publish repository")
        return
    }

    val projectIdVariable = gitLabConfig.projectIdEnv.get()
    val apiUrlVariable = gitLabConfig.apiUrlEnv.get()
    val projectId = environmentValueOf(projectIdVariable)
    val apiV4 = environmentValueOf(apiUrlVariable)
    if (projectId == null || apiV4 == null) {
        throw unbuildableRegistryUrlFailure(tokenVariable, projectIdVariable, projectId, apiUrlVariable, apiV4)
    }

    publishing.repositories.maven {
        name = gitLabConfig.name.orNull ?: DEFAULT_GITLAB_REPOSITORY_NAME
        url = URI("$apiV4/projects/$projectId/packages/maven")

        credentials(HttpHeaderCredentials::class) {
            this.name = GitLabHeader.JOB_TOKEN
            this.value = jobToken
        }

        authentication {
            create(GitLabHeader.AUTHENTICATION_NAME, HttpHeaderAuthentication::class.java)
        }
    }
}

private fun Project.environmentValueOf(variable: String): String? {
    val value = providers.environmentVariable(variable).orNull
    return value?.takeIf { it.isNotBlank() }
}

private fun Project.unbuildableRegistryUrlFailure(
    tokenVariable: String,
    projectIdVariable: String,
    projectId: String?,
    apiUrlVariable: String,
    apiV4: String?
): GradleException = GradleException(
    """
        Kreate's GitLab publishing is enabled and '$tokenVariable' is set, but the
        registry URL for project '$path' cannot be built:

            $projectIdVariable = ${projectId ?: UNSET_DESCRIPTION}
            $apiUrlVariable    = ${apiV4 ?: UNSET_DESCRIPTION}

        GitLab CI injects both automatically. Set them explicitly when publishing from
        somewhere else, or point Kreate at your own variable names with
        `gitlab { projectIdEnv = "…"; apiUrlEnv = "…" }`.
    """.trimIndent()
)
