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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile

private const val GIT_TIMEOUT_SECONDS: Long = 60

private const val NOTICE_LINE: String = "Copyright 2026 Example"

private val BRANCH_BUILD: Map<String, String> = mapOf("GITHUB_REF_TYPE" to "branch", "GITHUB_REF_NAME" to "develop")

private fun git(directory: File, vararg arguments: String): String {
    val identity = listOf("-c", "user.name=Kreate", "-c", "user.email=kreate@example.com")
    val unsigned = listOf("-c", "commit.gpgsign=false", "-c", "tag.gpgsign=false")
    val builder = ProcessBuilder(listOf("git") + identity + unsigned + arguments)
    builder.directory(directory)
    builder.redirectErrorStream(true)
    val process = builder.start()
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor(GIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    check(process.exitValue() == 0) { "git ${arguments.joinToString(" ")} failed: $output" }
    return output
}

private fun tagBuild(tag: String): Map<String, String> = mapOf("GITHUB_REF_TYPE" to "tag", "GITHUB_REF_NAME" to tag)

private fun jarEntries(jar: File): List<String> = JarFile(jar).use { file ->
    val entries = file.entries().toList()
    entries.map { entry -> entry.name }
}

private fun KreateBuildFixture.builtJar(): File {
    val libraries = file("build/libs").listFiles()
    return libraries.orEmpty().single()
}

private fun manifestOf(jar: File): Map<String, String> = JarFile(jar).use { file ->
    val attributes = file.manifest.mainAttributes
    attributes.entries.associate { (key, value) -> key.toString() to value.toString() }
}

class BuildConventionsFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()

    fun newBuild(kreateBlock: String, extra: String = ""): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.writeBuild(kreateBlock, extra = extra)
        return fixture
    }

    context("the license header check") {
        val block = """
            ${KreateBuildFixture.platformBlock}

            project {
                licenseHeader {
                    enabled = true
                    apacheLicense("$NOTICE_LINE")
                }
            }
        """.trimIndent()

        test("runs on check and names every file without the header") {
            val fixture = newBuild(block)
            fixture.writeKotlin("com/example/Sample.kt", "package com.example\n\nclass Sample")
            fixture.write("gradle.properties", "org.gradle.jvmargs=-Xmx1g")

            val result = fixture.buildAndFail("check", "-x", "test")

            result.output shouldContain "file(s) do not start with the license header"
            result.output shouldContain "  build.gradle.kts"
            result.output shouldContain "  gradle.properties"
            result.output shouldContain "  src/main/kotlin/com/example/Sample.kt"
        }

        test("passes when every file starts with the header in its comment style") {
            val fixture = newBuild(block)
            val buildScript = fixture.file("build.gradle.kts")
            val kotlinHeader = "/*\n" + apacheBlock() + " */\n"
            buildScript.writeText(kotlinHeader + "\n" + buildScript.readText())
            val settings = fixture.file("settings.gradle.kts")
            settings.writeText(kotlinHeader + "\n" + settings.readText())
            fixture.writeKotlin("com/example/Sample.kt", kotlinHeader + "\npackage com.example\n\nclass Sample")
            fixture.write("config.yaml", "#\n" + apacheHash() + "#\n\nkey: value")

            val result = fixture.build("kreateLicenseHeaderCheck")

            result.output shouldContain "file(s) start with the license header."
        }
    }

    context("the release guard") {
        fun repository(version: String): KreateBuildFixture {
            val fixture = newBuild(
                """
                ${KreateBuildFixture.platformBlock}

                project {
                    version { snapshotUnlessTagged = true }

                    publish {
                        enabled = true
                        releaseGuard { enabled = true }
                    }
                }
                """.trimIndent()
            )
            fixture.write("gradle.properties", "version=$version")
            fixture.write(".gitignore", "build/\n.gradle/\n.kotlin/\nlocal-state/\nmaven-local/\n")
            val directory = fixture.file(".")
            git(directory, "init", "-q", "-b", "main")
            git(directory, "add", "-A")
            git(directory, "commit", "-q", "-m", "feat: start")
            git(directory, "update-ref", "refs/remotes/origin/main", "HEAD")
            return fixture
        }

        test("lets a branch build publish its snapshot") {
            val fixture = repository("1.0.0")

            val result = fixture.buildWithEnvironment(BRANCH_BUILD, "kreateReleaseGuard")

            result.output shouldContain "Version 1.0.0-SNAPSHOT may be published."
        }

        test("lets a tag on main publish its release") {
            val fixture = repository("1.0.0")

            val result = fixture.buildWithEnvironment(tagBuild("1.0.0"), "kreateReleaseGuard")

            result.output shouldContain "Version 1.0.0 may be published."
        }

        test("refuses a tag that names another version") {
            val fixture = repository("1.0.0")
            fixture.withEnvironment("GITHUB_REF_TYPE", "tag")
            fixture.withEnvironment("GITHUB_REF_NAME", "1.0.1")

            val result = fixture.buildAndFail("kreateReleaseGuard")

            result.output shouldContain "The release tag \"1.0.1\" does not match the version \"1.0.0\""
        }

        test("refuses a tag on a commit that is not part of main") {
            val fixture = repository("1.0.0")
            val directory = fixture.file(".")
            git(directory, "checkout", "-q", "-b", "feature")
            git(directory, "commit", "-q", "--allow-empty", "-m", "feat: aside")
            fixture.withEnvironment("GITHUB_REF_TYPE", "tag")
            fixture.withEnvironment("GITHUB_REF_NAME", "1.0.0")

            val result = fixture.buildAndFail("kreateReleaseGuard")

            result.output shouldContain "is on a commit that is not part of origin/main"
        }

        test("refuses a version the environment changed") {
            val fixture = repository("1.0.0")
            fixture.withEnvironment("GITHUB_REF_TYPE", "branch")
            fixture.withEnvironment("VERSION", "6.6.6")

            val result = fixture.buildAndFail("kreateReleaseGuard")

            result.output shouldContain "resolved the version \"6.6.6\", but the build declares \"1.0.0\""
        }
    }

    context("the Java module") {
        test("compiles a descriptor that exports Kotlin packages, with javac warnings as errors") {
            val fixture = newBuild(
                """
                platform {
                    javaVersion = JavaVersion.VERSION_${KreateBuildFixture.javaVersion}
                    explicitApi = false
                    allWarningsAsErrors = true

                    compiler {
                        progressiveMode = true
                        extraWarnings = true
                    }

                    jvm {
                        moduleName = "com.example.sample"
                        javaWarningsAsErrors = true
                    }
                }

                project {
                    archives {
                        legalFiles.from("LICENSE")
                        implementationManifest = true
                        vendor = "Example"
                    }
                }
                """.trimIndent()
            )
            fixture.writeKotlin("com/example/sample/Greeting.kt", "package com.example.sample\n\nclass Greeting")
            fixture.write(
                "src/main/java/module-info.java",
                "module com.example.sample {\n    requires kotlin.stdlib;\n    exports com.example.sample;\n}"
            )
            fixture.write("LICENSE", "Example license")

            fixture.build("jar")

            val jar = fixture.builtJar()
            jarEntries(jar) shouldContain "module-info.class"
            jarEntries(jar) shouldContain "META-INF/LICENSE"
            val manifest = manifestOf(jar)
            manifest["Implementation-Title"] shouldBe "com.example.sample"
            manifest["Implementation-Vendor"] shouldBe "Example"
        }

        test("names an automatic module without a descriptor") {
            val fixture = newBuild(
                """
                ${KreateBuildFixture.platformBlock}

                platform {
                    jvm { moduleName = "com.example.sample" }
                }
                """.trimIndent()
            )
            fixture.writeKotlin("com/example/sample/Greeting.kt", "package com.example.sample\n\nclass Greeting")

            fixture.build("jar")

            val jar = fixture.builtJar()
            manifestOf(jar)["Automatic-Module-Name"] shouldBe "com.example.sample"
        }
    }

    context("the version floors") {
        test("raise a module on a tool classpath and leave the project classpath alone") {
            val fixture = newBuild(
                """
                ${KreateBuildFixture.platformBlock}

                project {
                    toolClasspaths {
                        floor("org.slf4j", "2.0.17")
                    }
                }
                """.trimIndent(),
                extra = """
                val dokkaProbe = configurations.create("dokkaProbe")
                val projectProbe = configurations.create("projectProbe")

                dependencies {
                    dokkaProbe("org.slf4j:slf4j-api:2.0.9")
                    projectProbe("org.slf4j:slf4j-api:2.0.9")
                }

                val toolVersions = dokkaProbe.incoming.resolutionResult.rootComponent.map { root ->
                    root.dependencies.filterIsInstance<org.gradle.api.artifacts.result.ResolvedDependencyResult>()
                        .joinToString { dependency -> dependency.selected.moduleVersion?.version.orEmpty() }
                }
                val projectVersions = projectProbe.incoming.resolutionResult.rootComponent.map { root ->
                    root.dependencies.filterIsInstance<org.gradle.api.artifacts.result.ResolvedDependencyResult>()
                        .joinToString { dependency -> dependency.selected.moduleVersion?.version.orEmpty() }
                }

                tasks.register("printProbe") {
                    val tool = toolVersions
                    val other = projectVersions
                    doLast {
                        println("tool: " + tool.get())
                        println("project: " + other.get())
                    }
                }
                """.trimIndent()
            )

            val result = fixture.build("printProbe")

            result.output shouldContain "tool: 2.0.17"
            result.output shouldContain "project: 2.0.9"
        }
    }
})

private fun apacheBlock(): String = apacheLines().joinToString("") { line -> " * $line".trimEnd() + "\n" }

private fun apacheHash(): String = apacheLines().joinToString("") { line -> "# $line".trimEnd() + "\n" }

private fun apacheLines(): List<String> = listOf(
    NOTICE_LINE,
    "",
    "Licensed under the Apache License, Version 2.0 (the \"License\");",
    "you may not use this file except in compliance with the License.",
    "You may obtain a copy of the License at",
    "",
    "    http://www.apache.org/licenses/LICENSE-2.0",
    "",
    "Unless required by applicable law or agreed to in writing, software",
    "distributed under the License is distributed on an \"AS IS\" BASIS,",
    "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.",
    "See the License for the specific language governing permissions and",
    "limitations under the License.",
)
