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
package com.davils.kreate.doctor.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.doctor.DoctorTaskNames
import com.davils.kreate.doctor.task.DoctorTask
import com.davils.kreate.feature.FeaturePhase
import com.davils.kreate.feature.KreateFeature
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

private const val ROOT_PATH: String = ":"

private const val CACHE_DIRECTORY: String = "caches/kreate"

private val DEFAULT_ENDPOINTS: Map<String, Pair<String, String>> = mapOf(
    "OSV.dev" to ("kreate.network.osvUrl" to "https://api.osv.dev/v1/vulns/OSV-0000-0"),
    "deps.dev" to ("kreate.network.depsDevUrl" to "https://api.deps.dev/v3/systems/maven/packages/kreate%3Adoctor"),
    "Maven Central" to ("" to "https://repo.maven.apache.org/maven2/"),
    "Trivy releases" to ("kreate.trivy.downloadBaseUrl" to "https://github.com/aquasecurity/trivy/releases")
)

internal object DoctorFeature : KreateFeature {
    override val phase: FeaturePhase = FeaturePhase.ASSURANCE

    override fun apply(project: Project, extension: KreateExtension) {
        if (project.path != ROOT_PATH) return
        val trivy = extension.trivy
        val owner = project

        project.tasks.register<DoctorTask>(DoctorTaskNames.DOCTOR) {
            gradleVersion.set(owner.gradle.gradleVersion)
            projectDirectory.set(owner.layout.projectDirectory)
            trivyEnabled.set(trivy.enabled)
            trivyVersion.set(trivy.version)
            trivyUseInstalled.set(trivy.useInstalled)
            cacheDirectory.set(owner.gradle.gradleUserHomeDir.resolve(CACHE_DIRECTORY))
            httpsProxy.set(owner.providers.environmentVariable("HTTPS_PROXY"))
            noProxy.set(owner.providers.environmentVariable("NO_PROXY"))
            proxyHost.set(owner.providers.systemProperty("https.proxyHost"))
            proxyPort.set(owner.providers.systemProperty("https.proxyPort"))
            offline.set(owner.gradle.startParameter.isOffline)
            endpoints.set(owner.provider { owner.endpoints() })
        }
    }

    private fun Project.endpoints(): Map<String, String> = DEFAULT_ENDPOINTS.mapValues { (_, source) ->
        val (property, fallback) = source
        providers.gradleProperty(property).orNull?.takeIf { property.isNotEmpty() } ?: fallback
    }
}
