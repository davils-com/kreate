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
import com.davils.kreate.module.project.publish.extension.pom.PomExtension
import org.gradle.api.publish.maven.MavenPom

/**
 * Configures a Maven POM with project and publishing information.
 *
 * This function maps various properties from the [PublishExtension] to the
 * [MavenPom], including project name, description, website, issue management,
 * CI management, licenses, developers, and SCM details.
 *
 * @param publishConfig The publishing configuration extension.
 * @param projectName The optional name of the project.
 * @param projectDescription The optional description of the project.
 * @since 1.0.0
 */
internal fun MavenPom.configurePom(
    publishConfig: PublishExtension,
    projectName: String?,
    projectDescription: String?
) {
    val pomConfig = publishConfig.pom

    projectName?.let { name.set(it) }
    projectDescription?.let { description.set(it) }
    publishConfig.inceptionYear.orNull?.let { inceptionYear.set(it.toString()) }
    publishConfig.website.orNull?.let { url.set(it) }

    configureIssueManagement(pomConfig)
    configureCiManagement(pomConfig)
    configureLicenses(pomConfig)
    configureDevelopers(pomConfig)
    configureScm(pomConfig)
}

private fun MavenPom.configureIssueManagement(pomConfig: PomExtension) {
    issueManagement {
        pomConfig.issueManagement.system.orNull?.let { value -> system.set(value) }
        pomConfig.issueManagement.url.orNull?.let { value -> url.set(value) }
    }
}

private fun MavenPom.configureCiManagement(pomConfig: PomExtension) {
    ciManagement {
        pomConfig.ciManagement.system.orNull?.let { value -> system.set(value) }
        pomConfig.ciManagement.url.orNull?.let { value -> url.set(value) }
    }
}

private fun MavenPom.configureLicenses(pomConfig: PomExtension) {
    licenses {
        license {
            pomConfig.licenses.license.name.orNull?.let { value -> name.set(value) }
            pomConfig.licenses.license.url.orNull?.let { value -> url.set(value) }
            pomConfig.licenses.license.distribution.orNull?.let { value -> distribution.set(value) }
        }
    }
}

private fun MavenPom.configureDevelopers(pomConfig: PomExtension) {
    developers {
        developer {
            pomConfig.developers.developer.id.orNull?.let { value -> id.set(value) }
            pomConfig.developers.developer.name.orNull?.let { value -> name.set(value) }
            pomConfig.developers.developer.email.orNull?.let { value -> email.set(value) }
            pomConfig.developers.developer.organization.orNull?.let { value -> organization.set(value) }
            pomConfig.developers.developer.timezone.orNull?.let { value -> timezone.set(value) }
        }
    }
}

private fun MavenPom.configureScm(pomConfig: PomExtension) {
    scm {
        pomConfig.scm.url.orNull?.let { value -> url.set(value) }
        pomConfig.scm.connection.orNull?.let { value -> connection.set(value) }
        pomConfig.scm.developerConnection.orNull?.let { value -> developerConnection.set(value) }
    }
}
