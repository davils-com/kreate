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
import com.davils.kreate.module.project.publish.extension.repository.GitlabExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.authentication.http.HttpHeaderAuthentication
import org.gradle.kotlin.dsl.credentials
import org.gradle.kotlin.dsl.withType
import java.net.URI

/**
 * The repository name used when the consumer did not name the GitLab target itself.
 *
 * @since 1.0.0
 */
internal const val DEFAULT_GITLAB_REPOSITORY_NAME: String = "GitlabPackageRegistry"

/**
 * The name of the publication Kreate registers when the consumer has none of its own.
 *
 * @since 2.0.1
 */
internal const val DEFAULT_PUBLICATION_NAME: String = "maven"

/**
 * Configures publishing to GitLab Package Registry.
 *
 * This registers a [MavenPublication] for the project's own component, points a Maven
 * repository at the registry of the current CI project, and configures the POM metadata.
 *
 * The publication is registered even outside CI, so that `publishToMavenLocal` produces the
 * same artifacts a pipeline would upload. Only the remote repository depends on the CI job
 * token being present.
 *
 * @param kreateExtension The Kreate configuration extension.
 * @since 1.0.0
 */
internal fun Project.configureGitlab(
    kreateExtension: KreateExtension,
) {
    val publishConfig = kreateExtension.project.publish
    val gitlabConfig = publishConfig.repositories.gitlab
    if (!gitlabConfig.enabled.get()) return

    val projectName = kreateExtension.project.name.orNull ?: project.name
    val projectDescription = kreateExtension.project.description.orNull

    extensions.configure<PublishingExtension>("publishing") {
        registerDefaultPublication(this, projectName)
        addGitlabRepository(this, gitlabConfig)

        publications.withType<MavenPublication>().configureEach {
            pom {
                configurePom(publishConfig, projectName, projectDescription)
            }
        }
    }
}

/**
 * Registers the publication that carries this project's artifacts.
 *
 * Nothing is registered when the consumer already declared a publication of their own -
 * that includes the one the Maven Central plugin creates, so both targets share a single
 * publication rather than uploading the same coordinates twice.
 *
 * @param publishing The publishing extension of this project.
 * @param artifactId The artifact id to publish under.
 * @since 2.0.1
 */
private fun Project.registerDefaultPublication(
    publishing: PublishingExtension,
    artifactId: String,
) {
    if (!publishing.publications.withType<MavenPublication>().isEmpty()) return

    // `java` covers every JVM and Kotlin/JVM library, `javaPlatform` a BOM. Anything else -
    // a Kotlin Multiplatform target, for instance - brings its own publications along and is
    // therefore already covered by the check above.
    val component = components.findByName("java") ?: components.findByName("javaPlatform")
    if (component == null) {
        logger.warn(
            "Kreate's GitLab publishing is enabled for project '$path', but the project " +
                "publishes no software component ('java' or 'javaPlatform'). Apply a plugin " +
                "that provides one, or declare a publication yourself - `publish` will " +
                "otherwise succeed without uploading anything."
        )
        return
    }

    // Sources are part of what makes an internally published library usable. Gradle ignores a
    // repeated call, so a consumer that already asked for them is unaffected. A `java-platform`
    // has no sources and no `java` extension, hence the guard.
    if (plugins.hasPlugin("java")) {
        extensions.configure<JavaPluginExtension>("java") {
            withSourcesJar()
        }
    }

    publishing.publications.register(DEFAULT_PUBLICATION_NAME, MavenPublication::class.java) {
        from(component)
        this.artifactId = artifactId
    }
}

/**
 * Adds the GitLab Package Registry of the current CI project as a publish target.
 *
 * Outside a pipeline there is no job token, and the repository is skipped rather than
 * registered with credentials that cannot work.
 *
 * @param publishing The publishing extension of this project.
 * @param gitlabConfig The GitLab repository configuration.
 * @since 1.0.0
 */
private fun Project.addGitlabRepository(
    publishing: PublishingExtension,
    gitlabConfig: GitlabExtension,
) {
    val tokenEnvName = gitlabConfig.tokenEnv.get()
    val jobToken = System.getenv(tokenEnvName)

    if (jobToken.isNullOrBlank()) {
        logger.lifecycle("No CI job token found in $tokenEnvName, skipping GitLab publish repository")
        return
    }

    val projectIdEnvName = gitlabConfig.projectIdEnv.get()
    val apiUrlEnvName = gitlabConfig.apiUrlEnv.get()
    val projectId = System.getenv(projectIdEnvName)
    val apiV4 = System.getenv(apiUrlEnvName)

    // Without this the URL becomes "null/projects/null/packages/maven" and the upload fails
    // with an unrelated protocol error somewhere deep in the transport layer.
    if (projectId.isNullOrBlank() || apiV4.isNullOrBlank()) {
        throw GradleException(
            """
                Kreate's GitLab publishing is enabled and '$tokenEnvName' is set, but the
                registry URL for project '$path' cannot be built:

                    $projectIdEnvName = ${projectId.describe()}
                    $apiUrlEnvName    = ${apiV4.describe()}

                GitLab CI injects both automatically. Set them explicitly when publishing from
                somewhere else, or point Kreate at your own variable names with
                `gitlab { projectIdEnv = "…"; apiUrlEnv = "…" }`.
            """.trimIndent()
        )
    }

    publishing.repositories.maven {
        name = gitlabConfig.name.orNull ?: DEFAULT_GITLAB_REPOSITORY_NAME
        url = URI("$apiV4/projects/$projectId/packages/maven")

        // A CI job token authenticates through the `Job-Token` header. `HttpHeaderAuthentication`
        // accepts `HttpHeaderCredentials` and nothing else - pairing it with `PasswordCredentials`
        // fails the moment the repository is actually used.
        credentials(HttpHeaderCredentials::class) {
            this.name = "Job-Token"
            this.value = jobToken
        }

        authentication {
            create("header", HttpHeaderAuthentication::class.java)
        }
    }
}

/**
 * Renders an environment variable value for the diagnostic above.
 *
 * @return `unset` when there is nothing to show, the value otherwise.
 * @since 2.0.1
 */
private fun String?.describe(): String = if (isNullOrBlank()) "unset" else this
