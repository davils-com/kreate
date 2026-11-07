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
package com.davils.kreate.doctor.check

import java.net.URI
import java.net.URISyntaxException

private const val MIN_JAVA: Int = 25

private const val MIN_GRADLE_MAJOR: Int = 9

private const val MIN_GRADLE_MINOR: Int = 1

private const val TRIVY: String = "Trivy"

private const val PROXY: String = "Proxy"

internal object Doctor {
    fun checks(facts: DoctorFacts): List<DoctorCheck> {
        val runtime = listOf(javaOf(facts), gradleOf(facts), gitOf(facts))
        val trivy = listOfNotNull(trivyOf(facts.trivy))
        return runtime + trivy + proxyOf(facts.proxy) + networkOf(facts) + cacheOf(facts)
    }

    private fun javaOf(facts: DoctorFacts): DoctorCheck {
        val version = facts.javaVersion
        if (version < MIN_JAVA) return DoctorCheck("Java", DoctorStatus.FAIL, "Java $version; Kreate needs $MIN_JAVA.")
        return DoctorCheck("Java", DoctorStatus.OK, "Gradle runs on Java $version.")
    }

    private fun gradleOf(facts: DoctorFacts): DoctorCheck {
        val version = facts.gradleVersion
        val parts = version.split('.').map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val major = parts.getOrElse(0) { 0 }
        val minor = parts.getOrElse(1) { 0 }
        val olderMajor = major < MIN_GRADLE_MAJOR
        val olderMinor = major == MIN_GRADLE_MAJOR && minor < MIN_GRADLE_MINOR
        val tooOld = olderMajor || olderMinor
        if (tooOld) return DoctorCheck("Gradle", DoctorStatus.FAIL, "Gradle $version; Kreate needs 9.1.")
        return DoctorCheck("Gradle", DoctorStatus.OK, "Gradle $version.")
    }

    private fun gitOf(facts: DoctorFacts): DoctorCheck {
        val version = facts.gitVersion
            ?: return DoctorCheck("Git", DoctorStatus.WARN, "git is missing; changelog, semver and hooks need it.")
        return DoctorCheck("Git", DoctorStatus.OK, "$version.")
    }

    private fun trivyOf(trivy: TrivyFacts?): DoctorCheck? {
        if (trivy == null) return null
        if (trivy.useInstalled) return installedTrivyOf(trivy)
        val version = trivy.version
        if (trivy.cached) return DoctorCheck(TRIVY, DoctorStatus.OK, "$version is cached and verified on use.")
        return DoctorCheck(TRIVY, DoctorStatus.WARN, "$version is downloaded on first use; run kreateTrivyProvision.")
    }

    private fun installedTrivyOf(trivy: TrivyFacts): DoctorCheck {
        val installed = trivy.installed
            ?: return DoctorCheck(TRIVY, DoctorStatus.FAIL, "useInstalled is set, but no trivy is installed.")
        if (!installed.contains(trivy.version)) {
            return DoctorCheck(TRIVY, DoctorStatus.WARN, "The installed $installed is not the pinned ${trivy.version}.")
        }
        return DoctorCheck(TRIVY, DoctorStatus.OK, "The installed $installed matches the pin.")
    }

    private fun proxyOf(proxy: ProxyFacts): List<DoctorCheck> {
        val environment = proxy.environment?.let(::withoutCredentials)
        val system = proxy.systemHost?.let { host -> "$host:${proxy.systemPort.orEmpty()}" }
        if (environment == null && system == null) return listOf(DoctorCheck(PROXY, DoctorStatus.OK, "None."))
        if (system == null) {
            val message = "HTTPS_PROXY is $environment, but Gradle resolves dependencies without it. " +
                "Set systemProp.https.proxyHost and systemProp.https.proxyPort in gradle.properties."
            return listOf(DoctorCheck(PROXY, DoctorStatus.WARN, message))
        }
        if (environment == null) {
            val message = "Gradle uses the proxy $system, but Kreate's checks of OSV.dev, deps.dev and Trivy " +
                "read HTTPS_PROXY. Set it as well."
            return listOf(DoctorCheck(PROXY, DoctorStatus.WARN, message))
        }
        if (hostOf(environment) != proxy.systemHost) {
            return listOf(DoctorCheck(PROXY, DoctorStatus.WARN, "HTTPS_PROXY is $environment, Gradle uses $system."))
        }
        return listOf(DoctorCheck(PROXY, DoctorStatus.OK, "$environment for Gradle and Kreate."))
    }

    private fun networkOf(facts: DoctorFacts): List<DoctorCheck> {
        if (facts.offline) {
            return listOf(DoctorCheck("Network", DoctorStatus.WARN, "Offline; the online checks report incomplete."))
        }
        val failing = facts.unreachable.map { (name, problem) -> DoctorCheck(name, DoctorStatus.WARN, problem) }
        val passing = facts.reachable.map { name -> DoctorCheck(name, DoctorStatus.OK, "Reachable.") }
        return passing + failing
    }

    private fun cacheOf(facts: DoctorFacts): DoctorCheck {
        val problem = facts.cacheProblem ?: return DoctorCheck("Cache", DoctorStatus.OK, "Writable.")
        return DoctorCheck("Cache", DoctorStatus.FAIL, problem)
    }

    private fun withoutCredentials(url: String): String = try {
        val uri = URI(url)
        URI(uri.scheme, null, uri.host, uri.port, uri.path, null, null).toString()
    } catch (_: URISyntaxException) {
        "(unreadable)"
    }

    private fun hostOf(url: String): String? = try {
        URI(url).host
    } catch (_: URISyntaxException) {
        null
    }
}
