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

package com.davils.kreate.functional

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.util.Base64

class MavenRepositoryPublishFunctionalTest : FunSpec({

    val workspace = kreateWorkspace()

    val publishTask = "publishAllPublicationsToDavilsRepository"

    fun newFixture(): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings(projectName = "sample-library")
        fixture.writeKotlin(
            "com/example/Sample.kt",
            """
            package com.example

            class Sample
            """.trimIndent()
        )
        return fixture
    }

    fun mavenBlock(target: String, otherTargets: String = "") = """
        ${KreateBuildFixture.platformBlock}

        project {
            description = "Fixture"

            publish {
                enabled = true
                website = "https://example.com"

                repositories {
                    maven {
                        name = "Davils"
                        usernameEnv = "KREATE_TEST_MAVEN_NAME"
                        passwordEnv = "KREATE_TEST_MAVEN_SECRET"
                        $target
                    }
                    $otherTargets
                }
            }
        }
    """.trimIndent()

    fun remoteTarget() = """
        releases = "https://repo.example.com/releases"
        snapshots = "https://repo.example.com/snapshots"
    """.trimIndent()

    fun KreateBuildFixture.localTarget(): String {
        val releases = file("repository/releases").toURI()
        val snapshots = file("repository/snapshots").toURI()
        return """
            releases = "$releases"
            snapshots = "$snapshots"
        """.trimIndent()
    }

    fun KreateBuildFixture.withLogin() {
        withEnvironment("KREATE_TEST_MAVEN_NAME", "publisher")
        withEnvironment("KREATE_TEST_MAVEN_SECRET", "test-secret")
    }

    fun KreateBuildFixture.writeMavenBuild(target: String, otherTargets: String = "") {
        writeBuild(mavenBlock(target, otherTargets), extraPlugins = listOf("""`maven-publish`"""))
    }

    context("routing by version") {
        test("wires publish to an upload task named after the repository") {
            val fixture = newFixture()
            fixture.withLogin()
            fixture.writeMavenBuild(remoteTarget())

            val dryRunTaskGraph = fixture.build("publish", "--dry-run")

            dryRunTaskGraph.output shouldContain ":publishMavenPublicationToDavilsRepository"
        }

        test("uploads a release to the releases repository only") {
            val fixture = newFixture()
            fixture.writeMavenBuild(fixture.localTarget())

            fixture.build(publishTask, "-Pversion=1.0.0")

            val released = fixture.file("repository/releases/com/example/sample-library/1.0.0")
            released.resolve("sample-library-1.0.0.pom").isFile shouldBe true
            released.resolve("sample-library-1.0.0.jar").isFile shouldBe true
            fixture.file("repository/snapshots").exists() shouldBe false
        }

        test("uploads a snapshot asked for by property to the snapshots repository only") {
            val fixture = newFixture()
            fixture.writeMavenBuild(fixture.localTarget())

            fixture.build(publishTask, "-Pversion=1.0.0", "-Pkreate.snapshot=true")

            val snapshot = fixture.file("repository/snapshots/com/example/sample-library/1.0.0-SNAPSHOT")
            snapshot.resolve("maven-metadata.xml").isFile shouldBe true
            fixture.file("repository/releases").exists() shouldBe false
        }

        test("uploads a snapshot asked for by the environment to the snapshots repository") {
            val fixture = newFixture()
            fixture.withEnvironment("KREATE_SNAPSHOT", "true")
            fixture.writeMavenBuild(fixture.localTarget())

            fixture.build(publishTask, "-Pversion=1.0.0")

            val snapshot = fixture.file("repository/snapshots/com/example/sample-library/1.0.0-SNAPSHOT")
            snapshot.resolve("maven-metadata.xml").isFile shouldBe true
        }

        test("lets the DSL rule a snapshot out against the property") {
            val fixture = newFixture()
            val kreateBlock = """
                ${mavenBlock(fixture.localTarget())}

                project { version { snapshot = false } }
            """.trimIndent()
            fixture.writeBuild(kreateBlock, extraPlugins = listOf("""`maven-publish`"""))

            fixture.build(publishTask, "-Pversion=1.0.0", "-Pkreate.snapshot=true")

            fixture.file("repository/releases/com/example/sample-library/1.0.0").isDirectory shouldBe true
        }

        test("skips the repository when the URL for the kind of version is not declared") {
            val fixture = newFixture()
            fixture.withLogin()
            fixture.writeMavenBuild("""snapshots = "https://repo.example.com/snapshots"""")

            val result = fixture.build("publish", "--dry-run", "-Pversion=1.0.0")

            result.output shouldContain "declares no `releases` URL"
            result.output shouldNotContain "ToDavilsRepository"
        }
    }

    context("credentials") {
        test("sends the login as basic authentication with every upload") {
            val fixture = newFixture()
            fixture.withLogin()

            RecordingMavenServer().use { server ->
                fixture.writeMavenBuild(
                    """
                    releases = "${server.address}/releases"
                    allowInsecureProtocol = true
                    """.trimIndent()
                )

                fixture.build(publishTask, "-Pversion=1.0.0")

                val login = Base64.getEncoder().encodeToString("publisher:test-secret".toByteArray())
                server.uploads shouldContain "/releases/com/example/sample-library/1.0.0/sample-library-1.0.0.pom"
                server.authorizations.shouldNotBeEmpty()
                server.authorizations.distinct() shouldBe listOf("Basic $login")
            }
        }

        test("skips the repository when no credentials are set") {
            val fixture = newFixture()
            fixture.writeMavenBuild(remoteTarget())

            val result = fixture.build("publish", "--dry-run")

            result.output shouldContain "No credentials found in KREATE_TEST_MAVEN_NAME and KREATE_TEST_MAVEN_SECRET"
            result.output shouldNotContain "ToDavilsRepository"
        }

        test("fails with a readable message when only half of the credentials is set") {
            val fixture = newFixture()
            fixture.withEnvironment("KREATE_TEST_MAVEN_NAME", "publisher")
            fixture.writeMavenBuild(remoteTarget())

            val result = fixture.buildAndFail("publish", "--dry-run")

            result.output shouldContain "'KREATE_TEST_MAVEN_SECRET' is empty"
            result.output shouldNotContain "publisher"
        }
    }

    context("refused configurations") {
        test("fails when the target declares no URL at all") {
            val fixture = newFixture()
            fixture.writeMavenBuild("")

            val result = fixture.buildAndFail("publish", "--dry-run")

            result.output shouldContain "declares neither a `releases` nor a `snapshots` URL"
        }

        test("refuses plain http unless it is allowed") {
            val fixture = newFixture()
            fixture.withLogin()
            fixture.writeMavenBuild("""releases = "http://repo.example.com/releases"""")

            val result = fixture.buildAndFail("publish", "--dry-run")

            result.output shouldContain "the `releases` URL uses plain http"
        }

        test("refuses credentials inside the URL without printing them") {
            val fixture = newFixture()
            fixture.writeMavenBuild("""releases = "https://publisher:url-secret@repo.example.com/releases"""")

            val result = fixture.buildAndFail("publish", "--dry-run")

            result.output shouldContain "carries credentials in the URL"
            result.output shouldNotContain "url-secret"
        }

        test("refuses a repository name another target already uses") {
            val fixture = newFixture()
            fixture.withLogin()
            fixture.withEnvironment("KREATE_TEST_TOKEN", "test-job-token")
            fixture.withEnvironment("KREATE_TEST_PROJECT_ID", "4711")
            fixture.withEnvironment("KREATE_TEST_API_URL", "https://gitlab.example.com/api/v4")
            fixture.writeMavenBuild(
                remoteTarget(),
                """
                gitlab {
                    name = "Davils"
                    tokenEnv = "KREATE_TEST_TOKEN"
                    projectIdEnv = "KREATE_TEST_PROJECT_ID"
                    apiUrlEnv = "KREATE_TEST_API_URL"
                }
                """.trimIndent()
            )

            val result = fixture.buildAndFail("publish", "--dry-run")

            result.output shouldContain "a publish repository named 'Davils' already exists"
        }
    }

    context("other targets and signing") {
        test("publishes next to GitLab and Maven Central") {
            val fixture = newFixture()
            fixture.withLogin()
            fixture.withEnvironment("KREATE_TEST_TOKEN", "test-job-token")
            fixture.withEnvironment("KREATE_TEST_PROJECT_ID", "4711")
            fixture.withEnvironment("KREATE_TEST_API_URL", "https://gitlab.example.com/api/v4")
            fixture.writeMavenBuild(
                remoteTarget(),
                """
                gitlab {
                    name = "TestRegistry"
                    tokenEnv = "KREATE_TEST_TOKEN"
                    projectIdEnv = "KREATE_TEST_PROJECT_ID"
                    apiUrlEnv = "KREATE_TEST_API_URL"
                }
                mavenCentral { signPublications = false }
                """.trimIndent()
            )

            val dryRunTaskGraph = fixture.build("publish", "--dry-run")

            dryRunTaskGraph.output shouldContain "ToDavilsRepository"
            dryRunTaskGraph.output shouldContain "ToTestRegistryRepository"
            dryRunTaskGraph.output shouldContain "ToMavenCentralRepository"
        }

        test("leaves publications unsigned by default") {
            val fixture = newFixture()
            fixture.withLogin()
            fixture.writeMavenBuild(remoteTarget())

            val dryRunTaskGraph = fixture.build(publishTask, "--dry-run")

            dryRunTaskGraph.output shouldNotContain ":signMavenPublication"
        }

        test("signs every publication once the target asks for it") {
            val fixture = newFixture()
            fixture.withLogin()
            fixture.writeMavenBuild(
                """
                ${remoteTarget()}
                signPublications = true
                """.trimIndent()
            )

            val dryRunTaskGraph = fixture.build(publishTask, "--dry-run")

            dryRunTaskGraph.output shouldContain ":signMavenPublication"
        }

        test("keeps signing Maven Central publications") {
            val fixture = newFixture()
            fixture.writeBuild(
                """
                ${KreateBuildFixture.platformBlock}

                project {
                    description = "Fixture"

                    publish {
                        enabled = true
                        website = "https://example.com"

                        repositories { mavenCentral { } }
                    }
                }
                """.trimIndent()
            )

            val dryRunTaskGraph = fixture.build("publishToMavenCentral", "--dry-run")

            dryRunTaskGraph.output shouldContain ":signMavenPublication"
        }
    }
})
