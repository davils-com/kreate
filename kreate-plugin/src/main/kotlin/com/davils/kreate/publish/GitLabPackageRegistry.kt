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

package com.davils.kreate.publish

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.authentication.http.HttpHeaderAuthentication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.credentials
import java.net.URI

/**
 * Adds a GitLab Package Registry to resolve dependencies from.
 *
 * GitLab's Maven endpoint does not accept the username and password pair Gradle sends by default.
 * It wants a token in a header whose *name* depends on which kind of token it is, and getting
 * that combination wrong produces a `401` that says nothing about the cause. This function
 * assembles the working combination:
 *
 * - Inside a pipeline the job token from `CI_JOB_TOKEN` is used, sent as `Job-Token`. It is
 *   scoped to the running pipeline and expires with it, so nothing long-lived has to be stored.
 * - Outside one, a personal or group access token is read from a Gradle property first and an
 *   environment variable second, and sent as `Private-Token`. The property comes first so the
 *   credential can live in `~/.gradle/gradle.properties`, outside the repository.
 *
 * Usable from a settings file as well as a build script:
 *
 * ```kotlin
 * // settings.gradle.kts
 * dependencyResolutionManagement {
 *     repositories {
 *         mavenCentral()
 *         gitlabPackageRegistry(providers) {
 *             url = "https://gitlab.example.com/api/v4/groups/42/-/packages/maven"
 *             content { includeGroup("com.example") }
 *         }
 *     }
 * }
 * ```
 *
 * Declaring it in settings requires Kreate on the settings class path, since a project plugin is
 * not loaded yet at that point — see the documentation for the `buildscript` block that does it.
 *
 * @param providers The provider factory of the surrounding `Project` or `Settings`. Needed to
 * read the Gradle property, which is not reachable through the environment.
 * @param action Configures the registry. [GitLabRegistrySpec.url] is required.
 * @return The registered repository, so that further Gradle repository options can be applied.
 * @throws GradleException If no URL was configured.
 * @since 2.2.0
 */
public fun RepositoryHandler.gitlabPackageRegistry(
    providers: ProviderFactory,
    action: Action<GitLabRegistrySpec>
): MavenArtifactRepository {
    val spec = GitLabRegistrySpec()
    action.execute(spec)

    val endpoint = spec.url ?: throw missingUrlFailure()
    val jobToken = providers.environmentVariable(spec.jobTokenVariable).filter { it.isNotBlank() }
    val headerName = headerNameFor(jobToken.isPresent, spec)
    val token = jobToken.orElse(accessTokenOf(providers, spec)).orNull

    val repositoryAction = Action<MavenArtifactRepository> {
        name = spec.name
        setUrl(URI(endpoint))

        credentials(HttpHeaderCredentials::class) {
            name = headerName
            value = token
        }

        authentication {
            create<HttpHeaderAuthentication>(GitLabHeader.AUTHENTICATION_NAME)
        }

        spec.contentActions.forEach { contentAction -> content(contentAction) }
    }

    return maven(repositoryAction)
}

private fun accessTokenOf(providers: ProviderFactory, spec: GitLabRegistrySpec): Provider<String> {
    val fromVariable = providers.environmentVariable(spec.tokenVariable)
    val fromPropertyOrVariable = providers.gradleProperty(spec.tokenProperty).orElse(fromVariable)
    return fromPropertyOrVariable.filter { it.isNotBlank() }
}

private fun headerNameFor(usingJobToken: Boolean, spec: GitLabRegistrySpec): String {
    if (usingJobToken) return GitLabHeader.JOB_TOKEN
    return spec.tokenHeader
}

private fun missingUrlFailure(): GradleException = GradleException(
    """
        A GitLab package registry was declared without a URL.

        Set the Maven endpoint of the registry:

            gitlabPackageRegistry(providers) {
                url = "https://gitlab.example.com/api/v4/groups/<id>/-/packages/maven"
            }

        GitLab exposes group, project and instance level endpoints; which one you want
        depends on where the packages live.
    """.trimIndent()
)
