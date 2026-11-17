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

package com.davils.kreate.publish.guard

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private val RELEASED_TOOLS: Map<String, String> = mapOf("Kreate" to "4.3.0", "Kotlin Gradle plugin" to "2.4.21")

private fun candidate(
    declared: String? = "1.2.0",
    resolved: String = "1.2.0-SNAPSHOT",
    tag: String? = null,
    prefix: String = "",
    tools: Map<String, String> = RELEASED_TOOLS
): ReleaseCandidate = ReleaseCandidate(declared, resolved, tag, prefix, tools)

class ReleaseGuardRulesTest : FunSpec({
    context("a build without a release tag") {
        test("may publish the snapshot of the declared version") {
            ReleaseGuardRules.problemsOf(candidate()).shouldBeEmpty()
        }

        test("may not publish a release") {
            val problems = ReleaseGuardRules.problemsOf(candidate(resolved = "1.2.0"))

            problems.single() shouldContain "must publish a snapshot"
        }
    }

    context("the declared version") {
        test("must be what the build resolved") {
            val problems = ReleaseGuardRules.problemsOf(candidate(resolved = "9.9.9-SNAPSHOT"))

            problems.single() shouldContain "resolved the version '9.9.9', but the build declares '1.2.0'"
        }

        test("must exist") {
            ReleaseGuardRules.problemsOf(candidate(declared = null)).single() shouldContain "declares no version"
        }
    }

    context("a tag build") {
        test("may publish the release its tag names") {
            ReleaseGuardRules.problemsOf(candidate(resolved = "1.2.0", tag = "1.2.0")).shouldBeEmpty()
        }

        test("strips the tag prefix") {
            val tagged = candidate(resolved = "1.2.0", tag = "v1.2.0", prefix = "v")

            ReleaseGuardRules.problemsOf(tagged).shouldBeEmpty()
        }

        test("refuses a tag without the prefix") {
            val tagged = candidate(resolved = "1.2.0", tag = "1.2.0", prefix = "v")

            ReleaseGuardRules.problemsOf(tagged).single() shouldContain "does not start with 'v'"
        }

        test("refuses a tag that names another version") {
            val problems = ReleaseGuardRules.problemsOf(candidate(resolved = "1.2.0", tag = "1.3.0"))

            problems.single() shouldContain "does not match the version '1.2.0'"
        }

        test("refuses a snapshot, even when its tag names it") {
            val problems = ReleaseGuardRules.problemsOf(candidate(tag = "1.2.0-SNAPSHOT"))

            problems shouldHaveSize 1
            problems.single() shouldContain "cannot publish the snapshot '1.2.0-SNAPSHOT'"
        }

        test("refuses a version that is no Semantic Versioning release") {
            val tagged = candidate(declared = "1.02.0", resolved = "1.02.0", tag = "1.02.0")

            ReleaseGuardRules.problemsOf(tagged).single() shouldContain "is not a Semantic Versioning 2.0.0 release"
        }

        test("refuses snapshot tooling") {
            val tools = RELEASED_TOOLS + ("Detekt" to "2.0.0-SNAPSHOT")
            val problems = ReleaseGuardRules.problemsOf(candidate(resolved = "1.2.0", tag = "1.2.0", tools = tools))

            problems shouldContainExactly
                listOf("A release cannot be built with snapshot tooling: Detekt 2.0.0-SNAPSHOT.")
        }
    }

    context("a release version") {
        test("follows Semantic Versioning without build metadata") {
            listOf("0.0.1", "1.0.0", "1.2.3-rc.1", "1.0.0-alpha-1.x").forEach { version ->
                ReleaseGuardRules.isRelease(version) shouldBe true
            }
        }

        test("has no leading zeros, build metadata or missing parts") {
            listOf("01.0.0", "1.0", "1.0.0+build", "1.0.0-01", "v1.0.0", "1.0.0-").forEach { version ->
                ReleaseGuardRules.isRelease(version) shouldBe false
            }
        }
    }
})
