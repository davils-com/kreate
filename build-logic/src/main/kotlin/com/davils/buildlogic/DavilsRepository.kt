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

package com.davils.buildlogic

import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.authentication.http.BasicAuthentication
import java.net.URI
import org.gradle.api.Project as GradleProject

private const val BASIC_AUTHENTICATION_NAME: String = "basic"

private const val MARKER_PUBLICATION: String = "PluginMarker"

public fun davilsRepositoryUrl(version: String): String {
    if (version.endsWith(SNAPSHOT_SUFFIX)) return Project.Repository.SNAPSHOTS_URL
    return Project.Repository.RELEASES_URL
}

public fun davilsLoginProblem(username: String?, password: String?): String? {
    val missing = missingLoginVariables(username, password)
    if (missing.isEmpty()) return null

    val listed = missing.joinToString(" and ")
    return "Refusing to publish to ${Project.Repository.HOST} without $listed. The upload " +
        "needs both ${Project.Repository.USERNAME_VARIABLE}, the name of the repository token, " +
        "and ${Project.Repository.PASSWORD_VARIABLE}, its secret."
}

public fun registerDavilsRepository(project: GradleProject, version: String) {
    val username = environmentValue(project.providers, Project.Repository.USERNAME_VARIABLE)
    val password = environmentValue(project.providers, Project.Repository.PASSWORD_VARIABLE)

    val publishing = project.extensions.getByType(PublishingExtension::class.java)
    publishing.repositories.maven {
        name = Project.Repository.NAME
        url = URI(davilsRepositoryUrl(version))
        authenticate(this, username, password)
    }

    val uploads = project.tasks.withType(PublishToMavenRepository::class.java)
    val problem = davilsLoginProblem(username, password)
    uploads.configureEach { doFirst(RequireDavilsLogin(problem)) }

    val markerUploads = uploads.matching { upload -> upload.name.contains(MARKER_PUBLICATION) }
    val artifactUploads = uploads.matching { upload -> !upload.name.contains(MARKER_PUBLICATION) }
    markerUploads.configureEach { mustRunAfter(artifactUploads) }
}

private fun authenticate(repository: MavenArtifactRepository, username: String?, password: String?) {
    if (username == null || password == null) return

    repository.credentials(PasswordCredentials::class.java) {
        this.username = username
        this.password = password
    }
    repository.authentication {
        create(BASIC_AUTHENTICATION_NAME, BasicAuthentication::class.java)
    }
}

private fun missingLoginVariables(username: String?, password: String?): List<String> = buildList {
    if (username == null) add(Project.Repository.USERNAME_VARIABLE)
    if (password == null) add(Project.Repository.PASSWORD_VARIABLE)
}

private fun environmentValue(providers: ProviderFactory, variable: String): String? {
    val value = providers.environmentVariable(variable).orNull
    return value?.takeIf { it.isNotBlank() }
}
