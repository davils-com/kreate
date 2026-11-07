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
package com.davils.kreate.governance.policy

import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.SourceLocation
import com.davils.kreate.assurance.rule.ruleFinding
import java.io.IOException
import java.io.StringReader
import java.net.URI
import java.net.URISyntaxException
import java.util.Properties

private const val STRICT_LOCK_MODE: String = "STRICT"

private const val STRICT_VERIFICATION: String = "STRICT"

private const val VERIFICATION_FILE: String = "gradle/verification-metadata.xml"

private const val WRAPPER_FILE: String = "gradle/wrapper/gradle-wrapper.properties"

private const val LOCK_FILE: String = "gradle.lockfile"

private const val VERIFICATION_RULE: String = "VERIFICATION_DISABLED"

private const val MAVEN_LOCAL_SUFFIX: String = "/.m2/repository"

private val VERIFYING: Regex = Regex("<verify-(metadata|signatures)>\\s*true\\s*</verify-")

internal class BuildPolicy(private val settings: PolicySettings) {
    fun findingsOf(repositories: List<RepositoryDeclaration>, state: BuildState): List<Finding> =
        repositoriesOf(repositories) + lockingOf(state) + verificationOf(state) + wrapperOf(state)

    private fun repositoriesOf(repositories: List<RepositoryDeclaration>): List<Finding> {
        val distinct = repositories.distinctBy { repository -> repository.url ?: repository.name }
        return distinct.flatMap { repository -> repositoryFindingsOf(repository) }
    }

    private fun repositoryFindingsOf(repository: RepositoryDeclaration): List<Finding> {
        val url = repository.url.orEmpty()
        val attributes = mapOf("repository" to repository.name, "url" to url)
        val findings = mutableListOf<Finding>()
        val name = repository.name
        if (repository.allowsInsecureProtocol) {
            findings += ruleFinding("INSECURE_REPOSITORY", null, "$name allows $url without TLS", attributes)
        }
        if (url.trimEnd('/').endsWith(MAVEN_LOCAL_SUFFIX)) {
            findings += ruleFinding("MAVEN_LOCAL_REPOSITORY", null, "$name is the local Maven repository", attributes)
        }
        if (!isAllowedHost(url)) {
            findings += ruleFinding("DISALLOWED_REPOSITORY", null, "${repository.name} points to $url", attributes)
        }
        return findings
    }

    private fun isAllowedHost(url: String): Boolean {
        if (settings.allowedRepositoryHosts.isEmpty()) return true
        val host = hostOf(url) ?: return false
        return settings.allowedRepositoryHosts.any { allowed -> host == allowed || host.endsWith(".$allowed") }
    }

    private fun hostOf(url: String): String? = try {
        URI(url).host?.lowercase()
    } catch (_: URISyntaxException) {
        null
    }

    private fun lockingOf(state: BuildState): List<Finding> {
        if (!settings.requireLocking || !state.hasExternalModules) return emptyList()
        val location = SourceLocation(LOCK_FILE)
        if (!state.lockFilePresent) {
            return listOf(ruleFinding("LOCKING_DISABLED", null, "The project has no lock file", location = location))
        }
        if (state.lockMode == STRICT_LOCK_MODE) return emptyList()
        val title = "Dependency locking runs in mode ${state.lockMode}"
        return listOf(ruleFinding("LENIENT_LOCKING", null, title, location = location))
    }

    private fun verificationOf(state: BuildState): List<Finding> {
        if (!settings.requireVerification) return emptyList()
        val location = SourceLocation(VERIFICATION_FILE)
        val metadata = state.verificationMetadata
            ?: return listOf(missingVerification(location))
        if (!VERIFYING.containsMatchIn(metadata)) {
            val title = "$VERIFICATION_FILE verifies neither checksums nor signatures"
            return listOf(ruleFinding(VERIFICATION_RULE, null, title, location = location))
        }
        if (state.verificationMode == STRICT_VERIFICATION) return emptyList()
        val title = "Dependency verification runs in mode ${state.verificationMode}"
        return listOf(ruleFinding(VERIFICATION_RULE, null, title, location = location))
    }

    private fun missingVerification(location: SourceLocation): Finding =
        ruleFinding(VERIFICATION_RULE, null, "The build has no $VERIFICATION_FILE", location = location)

    private fun wrapperOf(state: BuildState): List<Finding> {
        if (!settings.requireVerification) return emptyList()
        val text = state.wrapperProperties ?: return emptyList()
        val properties = propertiesOf(text)
        val location = SourceLocation(WRAPPER_FILE)
        val findings = mutableListOf<Finding>()
        val url = properties.getProperty("distributionUrl").orEmpty()
        if (url.startsWith("http:")) {
            findings += ruleFinding("INSECURE_WRAPPER", null, "The distribution comes from $url", location = location)
        }
        if (properties.getProperty("distributionSha256Sum").isNullOrBlank()) {
            val title = "The wrapper names no distributionSha256Sum"
            findings += ruleFinding("WRAPPER_UNVERIFIED", null, title, location = location)
        }
        return findings
    }

    private fun propertiesOf(text: String): Properties {
        val properties = Properties()
        try {
            properties.load(StringReader(text))
        } catch (_: IOException) {
            return properties
        } catch (_: IllegalArgumentException) {
            return properties
        }
        return properties
    }
}
