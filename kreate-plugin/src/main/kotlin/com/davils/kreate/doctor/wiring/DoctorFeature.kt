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

private const val ISO_OPEN_DATA: String = "https://isopublicstorageprod.blob.core.windows.net/opendata/_latest/"

private val STANDARDS_ENDPOINTS: Map<String, Pair<String, String>> = mapOf(
    "RFC Editor" to ("kreate.network.rfcEditorUrl" to "https://www.rfc-editor.org/rfc/rfc9562.json"),
    "IETF Datatracker" to ("kreate.network.datatrackerUrl" to "https://datatracker.ietf.org/doc/rfc9562/doc.json"),
    "W3C API" to ("kreate.network.w3cApiUrl" to "https://api.w3.org/"),
    "W3C" to ("kreate.network.w3cUrl" to "https://www.w3.org/"),
    "GitHub API" to ("kreate.network.githubApiUrl" to "https://api.github.com/"),
    "Unicode" to ("kreate.network.unicodeUrl" to "https://www.unicode.org/Public/UCD/latest/ReadMe.txt"),
    "ISO Open Data" to ("kreate.network.isoOpenDataUrl" to ISO_OPEN_DATA)
)

internal object DoctorFeature : KreateFeature {
    override val phase: FeaturePhase = FeaturePhase.ASSURANCE

    override fun apply(project: Project, extension: KreateExtension) {
        if (project.path != ROOT_PATH) return
        val trivy = extension.trivy
        val owner = project
        val assurance = extension.assurance
        val isCheckingStandards = assurance.enabled.get() && assurance.standards.enabled.get()

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
            endpoints.set(owner.provider { owner.endpoints(isCheckingStandards) })
        }
    }

    private fun Project.endpoints(isCheckingStandards: Boolean): Map<String, String> {
        val probed = DEFAULT_ENDPOINTS + STANDARDS_ENDPOINTS.takeIf { isCheckingStandards }.orEmpty()
        return probed.mapValues { (_, source) ->
            val (property, fallback) = source
            providers.gradleProperty(property).orNull?.takeIf { property.isNotEmpty() } ?: fallback
        }
    }
}
