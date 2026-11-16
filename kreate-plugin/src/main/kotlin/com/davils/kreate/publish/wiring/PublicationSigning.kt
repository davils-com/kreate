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
import com.davils.kreate.publish.MavenRepositoriesExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Project

internal fun Project.configureSigning(
    kreateExtension: KreateExtension,
) {
    val repositories = kreateExtension.project.publish.repositories
    if (!signsPublications(repositories)) return

    extensions.configure<MavenPublishBaseExtension>(MAVEN_PUBLISHING_EXTENSION_NAME) {
        signAllPublications()
    }
}

internal fun signsPublications(repositories: MavenRepositoriesExtension): Boolean {
    if (signsMavenTarget(repositories)) return true

    val mavenCentral = repositories.mavenCentral
    return mavenCentral.enabled.get() && mavenCentral.signPublications.get()
}

internal fun signsMavenTarget(repositories: MavenRepositoriesExtension): Boolean {
    val maven = repositories.maven
    return maven.enabled.get() && maven.signPublications.get()
}
