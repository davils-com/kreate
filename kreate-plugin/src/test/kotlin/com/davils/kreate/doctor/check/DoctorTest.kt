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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private val HEALTHY: DoctorFacts = DoctorFacts(
    javaVersion = 21,
    gradleVersion = "9.9.0",
    gitVersion = "git version 2.50.0",
    trivy = TrivyFacts("0.76.2", useInstalled = false, cached = true, installed = null),
    proxy = ProxyFacts(null, null, null),
    unreachable = emptyMap(),
    reachable = listOf("OSV.dev"),
    offline = false,
    cacheProblem = null
)

private fun statusOf(facts: DoctorFacts, name: String): DoctorCheck = Doctor.checks(facts).single { it.name == name }

class DoctorTest : FunSpec({
    context("a healthy machine") {
        test("passes every check") {
            val statuses = Doctor.checks(HEALTHY).map { check -> check.status }

            statuses.toSet() shouldBe setOf(DoctorStatus.OK)
        }
    }

    context("the runtime") {
        test("fails on an old Java or Gradle") {
            statusOf(HEALTHY.copy(javaVersion = 11), "Java").status shouldBe DoctorStatus.FAIL
            statusOf(HEALTHY.copy(gradleVersion = "8.14"), "Gradle").status shouldBe DoctorStatus.FAIL
        }

        test("warns without Git") {
            statusOf(HEALTHY.copy(gitVersion = null), "Git").status shouldBe DoctorStatus.WARN
        }
    }

    context("Trivy") {
        test("warns when it still has to be downloaded") {
            val trivy = TrivyFacts("0.76.2", useInstalled = false, cached = false, installed = null)

            statusOf(HEALTHY.copy(trivy = trivy), "Trivy").status shouldBe DoctorStatus.WARN
        }

        test("fails when the installed one is wanted but missing, and warns about another version") {
            val missing = TrivyFacts("0.76.2", useInstalled = true, cached = false, installed = null)
            val other = missing.copy(installed = "Version: 0.70.0")

            statusOf(HEALTHY.copy(trivy = missing), "Trivy").status shouldBe DoctorStatus.FAIL
            statusOf(HEALTHY.copy(trivy = other), "Trivy").status shouldBe DoctorStatus.WARN
        }
    }

    context("the proxy") {
        test("warns when Gradle does not know the proxy of the environment, without showing credentials") {
            val proxy = ProxyFacts("http://user:secret@proxy.corp:3128", null, null)

            val check = statusOf(HEALTHY.copy(proxy = proxy), "Proxy")

            check.status shouldBe DoctorStatus.WARN
            check.message shouldContain "http://proxy.corp:3128"
            check.message.contains("secret") shouldBe false
        }

        test("warns when only Gradle knows the proxy, and when both differ") {
            val onlyGradle = HEALTHY.copy(proxy = ProxyFacts(null, "proxy.corp", "3128"))

            statusOf(onlyGradle, "Proxy").status shouldBe DoctorStatus.WARN
            statusOf(HEALTHY.copy(proxy = ProxyFacts("http://a.corp:1", "b.corp", "1")), "Proxy").status shouldBe
                DoctorStatus.WARN
        }

        test("passes when both agree") {
            statusOf(HEALTHY.copy(proxy = ProxyFacts("http://a.corp:1", "a.corp", "1")), "Proxy").status shouldBe
                DoctorStatus.OK
        }
    }

    context("the network and the cache") {
        test("warn when offline or unreachable and fail without a writable cache") {
            statusOf(HEALTHY.copy(offline = true), "Network").status shouldBe DoctorStatus.WARN
            val unreachable = HEALTHY.copy(unreachable = mapOf("deps.dev" to "down"))

            statusOf(unreachable, "deps.dev").status shouldBe DoctorStatus.WARN
            statusOf(HEALTHY.copy(cacheProblem = "read-only"), "Cache").status shouldBe DoctorStatus.FAIL
        }
    }
})
