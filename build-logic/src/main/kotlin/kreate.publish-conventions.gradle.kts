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

import com.davils.buildlogic.LOCAL_PUBLISH_TASK
import com.davils.buildlogic.LOCAL_TASK_GROUP
import com.davils.buildlogic.LocalPublishTask
import com.davils.buildlogic.Project
import com.davils.buildlogic.SNAPSHOT_SUFFIX
import com.davils.buildlogic.compositeVersion
import com.davils.buildlogic.isContinuousIntegration
import com.davils.buildlogic.publishedPluginCoordinates
import com.davils.buildlogic.requestsLocalPublish

plugins {
    id("com.vanniktech.maven.publish")
    signing
}

version = compositeVersion(providers, layout.settingsDirectory)

val releaseTag = providers.environmentVariable("CI_COMMIT_TAG")
val taggedVersion = releaseTag.map { tag -> tag.removePrefix("v") }
if (taggedVersion.isPresent) {
    version = taggedVersion.get()
}

val isLocalPublish = requestsLocalPublish(gradle, providers)
val isSnapshot = version.toString().endsWith(SNAPSHOT_SUFFIX)
if (isLocalPublish && !isSnapshot) {
    version = "$version$SNAPSHOT_SUFFIX"
}

group = Project.Identity.GROUP

val defaultArtifactId = Project.Identity.NAME.lowercase()
val publishedArtifactId = providers.gradleProperty("kreate.publish.artifactId").getOrElse(defaultArtifactId)
val publishedName = providers.gradleProperty("kreate.publish.name").getOrElse(Project.Identity.NAME)
val publishedDescription = providers.gradleProperty("kreate.publish.description").getOrElse(Project.Identity.DESCRIPTION)

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(Project.Identity.GROUP, publishedArtifactId, version.toString())

    pom {
        name = publishedName
        description = publishedDescription
        inceptionYear = Project.Identity.INCEPTION_YEAR.toString()
        url = Project.Organization.WEBSITE_URL

        issueManagement {
            system = Project.IssueManagement.SYSTEM
            url = Project.IssueManagement.URL
        }

        ciManagement {
            system = Project.VersionControl.CI_SYSTEM
            url = Project.VersionControl.CI_URL
        }

        licenses {
            license {
                name = Project.Legal.LICENSE_NAME
                url = Project.Legal.LICENSE_URL
                distribution = Project.Legal.LICENSE_DISTRIBUTION
            }
        }

        developers {
            developer {
                id = Project.Organization.NAME.lowercase()
                name = Project.Organization.NAME
                email = Project.Organization.EMAIL
                organization = Project.Organization.NAME
                timezone = Project.Organization.TIMEZONE
            }
        }

        scm {
            url = Project.VersionControl.SCM_URL
            connection = Project.VersionControl.SCM_CONNECTION
            developerConnection = Project.VersionControl.SCM_DEVELOPER_CONNECTION
        }
    }
}

val primaryCoordinate = "${Project.Identity.GROUP}:$publishedArtifactId"

val resolvedVersion = version.toString()

tasks.register<LocalPublishTask>(LOCAL_PUBLISH_TASK) {
    group = LOCAL_TASK_GROUP
    description = "Installs this build and any plugin markers into the local Maven repository."

    dependsOn(tasks.named("publishToMavenLocal"))

    publishedGroup = Project.Identity.GROUP
    library = publishedArtifactId
    publishedVersion = resolvedVersion
    runningInCi = isContinuousIntegration(providers)
    gradleUserHome = layout.dir(provider { gradle.gradleUserHomeDir })
    repositoryDirectory = layout.settingsDirectory.dir("..")

    coordinates = publishedPluginCoordinates(project, primaryCoordinate)
}
