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

// The composite root's `gradle.properties` is the single source of truth for the version, and an
// included build does not inherit it. Without this line the plugin's version is `unspecified` in
// every invocation that does not set `CI_COMMIT_TAG` or pass `-Pversion=`.
version = compositeVersion(providers, layout.settingsDirectory)

// The tag when CI publishes a release, and otherwise whatever `gradle.properties` says. The
// fallback used to be a literal, which went stale the moment a release was tagged and left the
// two places that name a version disagreeing.
System.getenv("CI_COMMIT_TAG")?.removePrefix("v")?.let { tagged -> version = tagged }

// A local publish carries the snapshot suffix so that it cannot shadow a release. This has to be
// decided before `coordinates(...)` below reads the version, which rules out a task doing it.
val localPublish = requestsLocalPublish(gradle, providers)
if (localPublish && !version.toString().endsWith(SNAPSHOT_SUFFIX)) {
    version = "$version$SNAPSHOT_SUFFIX"
}

group = Project.Identity.GROUP

// What this build publishes. The defaults are the plugin's own coordinates, because for three
// major versions the plugin was the only artefact this repository produced. A sibling build states
// its own in its `gradle.properties`, which is per build and therefore cannot be read from the
// wrong project.
val publishedArtifactId = providers.gradleProperty("kreate.publish.artifactId")
    .getOrElse(Project.Identity.NAME.lowercase())
val publishedName = providers.gradleProperty("kreate.publish.name")
    .getOrElse(Project.Identity.NAME)
val publishedDescription = providers.gradleProperty("kreate.publish.description")
    .getOrElse(Project.Identity.DESCRIPTION)

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

// Read outside the task configuration action: inside it, `version` resolves to the task's own
// property rather than the project's.
val resolvedVersion = version.toString()

// Kreate cannot apply itself — `build-logic` compiles the conventions that build the plugin, so a
// dependency on the plugin's own artefact would be a cycle. This task is therefore the only
// hand-written copy of `kreateLocalPublish` in the repository; everywhere else the plugin
// registers it. See `com.davils.buildlogic.LocalDevelopment` for the shared contract.
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

    // Resolved in the task configuration action rather than at the top of the script: Gradle
    // realises a task once every project has been evaluated, so `gradlePlugin { plugins { } }`
    // in the consuming build file is complete by now.
    coordinates = publishedPluginCoordinates(project, primaryCoordinate)
}
