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

package com.davils.kreate.gradle

import com.davils.kreate.publish.PomDeveloperExtension
import com.davils.kreate.publish.PomExtension
import com.davils.kreate.publish.PomLicenseExtension
import com.davils.kreate.publish.PublishExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPomDeveloper
import org.gradle.api.publish.maven.MavenPomLicense

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
    val declared = pomConfig.licenses.license
    licenses {
        license { copyFrom(declared) }
    }
}

private fun MavenPomLicense.copyFrom(declared: PomLicenseExtension) {
    declared.name.orNull?.let { value -> name.set(value) }
    declared.url.orNull?.let { value -> url.set(value) }
    declared.distribution.orNull?.let { value -> distribution.set(value) }
}

private fun MavenPom.configureDevelopers(pomConfig: PomExtension) {
    val declared = pomConfig.developers.developer
    developers {
        developer { copyFrom(declared) }
    }
}

private fun MavenPomDeveloper.copyFrom(declared: PomDeveloperExtension) {
    declared.id.orNull?.let { value -> id.set(value) }
    declared.name.orNull?.let { value -> name.set(value) }
    declared.email.orNull?.let { value -> email.set(value) }
    declared.organization.orNull?.let { value -> organization.set(value) }
    declared.timezone.orNull?.let { value -> timezone.set(value) }
}

private fun MavenPom.configureScm(pomConfig: PomExtension) {
    scm {
        pomConfig.scm.url.orNull?.let { value -> url.set(value) }
        pomConfig.scm.connection.orNull?.let { value -> connection.set(value) }
        pomConfig.scm.developerConnection.orNull?.let { value -> developerConnection.set(value) }
    }
}
