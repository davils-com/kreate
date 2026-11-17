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

import com.davils.kreate.KREATE_VERSION
import com.davils.kreate.KreateExtension
import com.davils.kreate.project.wiring.ciReleaseTag
import com.davils.kreate.publish.ReleaseGuardTaskNames
import com.davils.kreate.publish.task.ReleaseGuardCheck
import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import dev.detekt.gradle.extensions.DetektExtension as KDetektExtension

private const val KREATE: String = "Kreate"

private const val KOTLIN: String = "Kotlin Gradle plugin"

private const val DETEKT: String = "Detekt"

internal fun Project.registerReleaseGuard(extension: KreateExtension) {
    val guard = extension.project.publish.releaseGuard
    if (!guard.enabled.get()) return
    val owner = this
    val versionProperty = extension.project.version.property.get()

    val check = tasks.register<ReleaseGuardCheck>(ReleaseGuardTaskNames.GUARD) {
        declaredVersion.set(owner.providers.gradleProperty(versionProperty))
        resolvedVersion.set(owner.provider { owner.version.toString() })
        tag.set(owner.ciReleaseTag())
        tagPrefix.set(guard.tagPrefix)
        releaseBranch.set(guard.releaseBranch)
        remote.set(guard.remote)
        tools.set(owner.toolVersions())
        projectDirectory.set(owner.layout.projectDirectory)
    }

    tasks.withType<PublishToMavenRepository>().configureEach { dependsOn(check) }
}

private fun Project.toolVersions(): Map<String, String> {
    val detekt = extensions.findByType(KDetektExtension::class.java)
    val detektVersion = detekt?.toolVersion?.orNull
    return buildMap {
        put(KREATE, KREATE_VERSION)
        put(KOTLIN, getKotlinPluginVersion())
        detektVersion?.let { version -> put(DETEKT, version) }
    }
}
