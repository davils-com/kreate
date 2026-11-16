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

package com.davils.kreate.publish.routing

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MavenRoutingTest : FunSpec({
    val releases = "https://repo.example.com/releases"
    val snapshots = "https://repo.example.com/snapshots"

    context("repositoryKindOf") {
        test("a plain version is a release") {
            repositoryKindOf("1.2.3") shouldBe RepositoryKind.RELEASE
        }

        test("a pre-release without the suffix is still a release") {
            repositoryKindOf("1.2.3-rc.1") shouldBe RepositoryKind.RELEASE
        }

        test("a version ending in the snapshot suffix is a snapshot") {
            repositoryKindOf("1.2.3-SNAPSHOT") shouldBe RepositoryKind.SNAPSHOT
        }
    }

    context("addressFor") {
        test("a release goes to the releases URL") {
            addressFor(RepositoryKind.RELEASE, releases, snapshots) shouldBe releases
        }

        test("a snapshot goes to the snapshots URL") {
            addressFor(RepositoryKind.SNAPSHOT, releases, snapshots) shouldBe snapshots
        }

        test("a snapshot never falls back to the releases URL") {
            addressFor(RepositoryKind.SNAPSHOT, releases, null) shouldBe null
        }

        test("a release never falls back to the snapshots URL") {
            addressFor(RepositoryKind.RELEASE, null, snapshots) shouldBe null
        }
    }

    context("addressProblemOf") {
        test("accepts https") {
            addressProblemOf(releases, allowInsecureProtocol = false) shouldBe null
        }

        test("accepts a local directory") {
            addressProblemOf("file:///tmp/repository", allowInsecureProtocol = false) shouldBe null
        }

        test("refuses plain http") {
            addressProblemOf("http://repo.example.com", allowInsecureProtocol = false) shouldBe AddressProblem.INSECURE
        }

        test("accepts plain http when it is allowed") {
            addressProblemOf("http://repo.example.com", allowInsecureProtocol = true) shouldBe null
        }

        test("refuses credentials inside the URL even over https") {
            val address = "https://user:secret@repo.example.com/releases"

            addressProblemOf(address, allowInsecureProtocol = true) shouldBe AddressProblem.EMBEDDED_CREDENTIALS
        }

        test("refuses any other scheme") {
            addressProblemOf("ftp://repo.example.com", allowInsecureProtocol = true) shouldBe
                AddressProblem.UNSUPPORTED_SCHEME
        }

        test("refuses a URL without a scheme") {
            addressProblemOf("repo.example.com/releases", allowInsecureProtocol = true) shouldBe
                AddressProblem.UNSUPPORTED_SCHEME
        }

        test("refuses text that is no URL") {
            addressProblemOf("https://repo example", allowInsecureProtocol = true) shouldBe AddressProblem.MALFORMED
        }
    }

    context("loginStateOf") {
        test("both halves make the login complete") {
            loginStateOf("name", "secret") shouldBe LoginState.COMPLETE
        }

        test("no half at all is anonymous") {
            loginStateOf(null, null) shouldBe LoginState.ANONYMOUS
        }

        test("a user name without a password is partial") {
            loginStateOf("name", null) shouldBe LoginState.PARTIAL
        }

        test("a password without a user name is partial") {
            loginStateOf(null, "secret") shouldBe LoginState.PARTIAL
        }
    }
})
