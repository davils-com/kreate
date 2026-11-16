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
import com.davils.kreate.gradle.declaredProjectName
import com.davils.kreate.publish.MavenTargetExtension
import com.davils.kreate.publish.routing.AddressProblem
import com.davils.kreate.publish.routing.LoginState
import com.davils.kreate.publish.routing.RepositoryKind
import com.davils.kreate.publish.routing.addressFor
import com.davils.kreate.publish.routing.addressProblemOf
import com.davils.kreate.publish.routing.loginStateOf
import com.davils.kreate.publish.routing.repositoryKindOf
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.authentication.http.BasicAuthentication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.credentials
import java.net.URI

private const val MAVEN_TARGET: String = "Maven repository"

private const val BASIC_AUTHENTICATION_NAME: String = "basic"

private const val LOCAL_DIRECTORY_SCHEME: String = "file"

internal fun Project.configureMaven(
    kreateExtension: KreateExtension,
) {
    val publishConfig = kreateExtension.project.publish
    val mavenConfig = publishConfig.repositories.maven
    if (!mavenConfig.enabled.get()) return

    val projectName = declaredProjectName(kreateExtension)
    val projectDescription = kreateExtension.project.description.orNull

    extensions.configure<PublishingExtension>(PUBLISHING_EXTENSION_NAME) {
        registerDefaultPublication(this, projectName, MAVEN_TARGET)
        addMavenRepository(this, mavenConfig)
        configurePublicationPoms(this, publishConfig, projectName, projectDescription)
    }
}

private fun Project.addMavenRepository(
    publishing: PublishingExtension,
    mavenConfig: MavenTargetExtension,
) {
    val address = routedAddress(mavenConfig) ?: return

    val usernameVariable = mavenConfig.usernameEnv.get()
    val passwordVariable = mavenConfig.passwordEnv.get()
    val username = environmentValueOf(usernameVariable)
    val password = environmentValueOf(passwordVariable)
    val state = loginStateOf(username, password)
    if (state == LoginState.PARTIAL) throw partialLoginFailure(usernameVariable, passwordVariable, username)

    val isRemote = address.scheme != LOCAL_DIRECTORY_SCHEME
    if (state == LoginState.ANONYMOUS && isRemote) {
        logger.lifecycle(
            "No credentials found in $usernameVariable and $passwordVariable, skipping Maven publish repository"
        )
        return
    }

    val repositoryName = mavenConfig.name.get()
    val existing = publishing.repositories.findByName(repositoryName)
    if (existing != null) throw duplicateRepositoryFailure(repositoryName)

    publishing.repositories.maven {
        name = repositoryName
        url = address
        isAllowInsecureProtocol = mavenConfig.allowInsecureProtocol.get()
        authenticate(this, username, password)
    }
}

private fun Project.routedAddress(mavenConfig: MavenTargetExtension): URI? {
    val releases = declaredAddress(mavenConfig.releases)
    val snapshots = declaredAddress(mavenConfig.snapshots)
    if (releases == null && snapshots == null) throw undeclaredAddressesFailure()

    val resolvedVersion = version.toString()
    val kind = repositoryKindOf(resolvedVersion)
    val address = addressFor(kind, releases, snapshots)
    if (address == null) {
        logger.lifecycle(
            "Version $resolvedVersion is a ${kind.label} and the Maven target declares no " +
                "`${kind.property}` URL, skipping Maven publish repository"
        )
        return null
    }

    val problem = addressProblemOf(address, mavenConfig.allowInsecureProtocol.get())
    if (problem != null) throw rejectedAddressFailure(kind, problem)
    return URI(address)
}

private fun declaredAddress(property: Property<String>): String? {
    val value = property.orNull
    return value?.takeIf { it.isNotBlank() }
}

private fun authenticate(repository: MavenArtifactRepository, username: String?, password: String?) {
    if (username == null || password == null) return

    repository.credentials(PasswordCredentials::class) {
        this.username = username
        this.password = password
    }
    repository.authentication {
        create<BasicAuthentication>(BASIC_AUTHENTICATION_NAME)
    }
}

private fun Project.undeclaredAddressesFailure(): GradleException = GradleException(
    """
        Kreate's Maven repository publishing is enabled for project '$path', but the target
        declares neither a `releases` nor a `snapshots` URL:

            kreate { project { publish { repositories {
                maven {
                    releases = "https://repo.example.com/releases"
                    snapshots = "https://repo.example.com/snapshots"
                }
            } } } }

        Declare at least the one this build publishes to.
    """.trimIndent()
)

private fun Project.rejectedAddressFailure(kind: RepositoryKind, problem: AddressProblem): GradleException =
    GradleException(
        "Kreate's Maven repository publishing is enabled for project '$path', but the " +
            "`${kind.property}` URL ${problem.reason}."
    )

private fun Project.partialLoginFailure(
    usernameVariable: String,
    passwordVariable: String,
    username: String?
): GradleException {
    val missingVariable = usernameVariable.takeIf { username == null } ?: passwordVariable
    return GradleException(
        """
            Kreate's Maven repository publishing is enabled for project '$path', but only one
            half of the credentials is set: '$missingVariable' is empty.

            Set both '$usernameVariable' and '$passwordVariable', or neither to skip the
            repository. Point Kreate at your own variable names with
            `maven { usernameEnv = "…"; passwordEnv = "…" }`.
        """.trimIndent()
    )
}

private fun Project.duplicateRepositoryFailure(repositoryName: String): GradleException = GradleException(
    "Kreate's Maven repository publishing is enabled for project '$path', but a publish " +
        "repository named '$repositoryName' already exists. Give the Maven target a name of " +
        "its own with `maven { name = \"…\" }`."
)
