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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private const val VERIFYING: String = "<verification-metadata><configuration><verify-metadata>true</verify-metadata>" +
    "</configuration></verification-metadata>"

private const val DISTRIBUTION: String =
    "distributionUrl=https\\://services.gradle.org/distributions/gradle-9.9.0-bin.zip"

private const val WRAPPER: String = "$DISTRIBUTION\ndistributionSha256Sum=0b00b0ff\n"

private val HARDENED: BuildState = BuildState(true, true, "STRICT", "STRICT", VERIFYING, WRAPPER)

private fun rulesOf(
    state: BuildState = HARDENED,
    repositories: List<RepositoryDeclaration> = emptyList(),
    settings: PolicySettings = PolicySettings()
): List<String> {
    val findings = BuildPolicy(settings).findingsOf(repositories, state)
    return findings.map { finding -> finding.ruleId }
}

class BuildPolicyTest : FunSpec({
    context("a hardened build") {
        test("passes") {
            rulesOf().shouldBeEmpty()
        }
    }

    context("locking") {
        test("is missing without a lock file") {
            rulesOf(HARDENED.copy(lockFilePresent = false)) shouldBe listOf("LOCKING_DISABLED")
        }

        test("is lenient outside the strict mode") {
            rulesOf(HARDENED.copy(lockMode = "DEFAULT")) shouldBe listOf("LENIENT_LOCKING")
        }

        test("does not matter without external modules") {
            rulesOf(HARDENED.copy(hasExternalModules = false, lockFilePresent = false)).shouldBeEmpty()
        }

        test("is not required when the policy says so") {
            val unlocked = HARDENED.copy(lockFilePresent = false)

            rulesOf(unlocked, settings = PolicySettings(requireLocking = false)).shouldBeEmpty()
        }
    }

    context("verification") {
        test("is missing without metadata") {
            rulesOf(HARDENED.copy(verificationMetadata = null)) shouldBe listOf("VERIFICATION_DISABLED")
        }

        test("is missing when the metadata verifies nothing") {
            val empty = HARDENED.copy(verificationMetadata = "<verification-metadata/>")

            rulesOf(empty) shouldBe listOf("VERIFICATION_DISABLED")
        }

        test("is missing when the build turns it off or makes it lenient") {
            rulesOf(HARDENED.copy(verificationMode = "LENIENT")) shouldBe listOf("VERIFICATION_DISABLED")
        }
    }

    context("the wrapper") {
        test("without a checksum is unverified") {
            val wrapper = "distributionUrl=https\\://services.gradle.org/distributions/gradle-9.9.0-bin.zip\n"

            rulesOf(HARDENED.copy(wrapperProperties = wrapper)) shouldBe listOf("WRAPPER_UNVERIFIED")
        }

        test("over http is insecure") {
            val wrapper = "distributionUrl=http\\://mirror.example/gradle.zip\ndistributionSha256Sum=ab\n"

            rulesOf(HARDENED.copy(wrapperProperties = wrapper)) shouldBe listOf("INSECURE_WRAPPER")
        }

        test("that does not exist is not judged") {
            rulesOf(HARDENED.copy(wrapperProperties = null)).shouldBeEmpty()
        }
    }

    context("repositories") {
        test("that allow plain http are insecure") {
            val repository = RepositoryDeclaration("legacy", "http://repo.example/maven", true)

            rulesOf(repositories = listOf(repository)) shouldBe listOf("INSECURE_REPOSITORY")
        }

        test("in the local Maven repository are reported") {
            val repository = RepositoryDeclaration("MavenLocal", "file:/home/user/.m2/repository/", false)

            rulesOf(repositories = listOf(repository)) shouldBe listOf("MAVEN_LOCAL_REPOSITORY")
        }

        test("on a host outside the allowed list are reported") {
            val settings = PolicySettings(allowedRepositoryHosts = setOf("repo.maven.apache.org"))
            val central = RepositoryDeclaration("central", "https://repo.maven.apache.org/maven2", false)
            val other = RepositoryDeclaration("jitpack", "https://jitpack.io", false)

            rulesOf(repositories = listOf(central, other), settings = settings) shouldBe listOf("DISALLOWED_REPOSITORY")
        }
    }
})
