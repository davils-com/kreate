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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

/**
 * A throwaway Gradle project that applies the Kreate plugin, driven through TestKit.
 *
 * The fixture writes a complete, self-contained build so that each test exercises the
 * plugin the way a consumer would: through real Gradle task execution rather than through
 * an in-memory project model. That is the only way to observe the properties this suite
 * cares about — up-to-date behaviour, task ordering, and configuration cache reuse.
 *
 * @param rootDirectory The temporary directory the project is written into.
 */
class KreateBuildFixture(
    val rootDirectory: File,
    private val gradleVersion: String? = null
) {

    /**
     * Extra environment variables handed to the build.
     *
     * Kreate reads the CI coordinates of a publish target from the environment at
     * configuration time, so a test that covers publishing has to be able to set them.
     */
    private val environment: MutableMap<String, String> = mutableMapOf()

    /**
     * The native project directory used by the JNI tests.
     */
    val nativeProjectDirectory: File get() = rootDirectory.resolve("jni/sample")

    /**
     * The local Maven repository the build publishes into and resolves from.
     *
     * Redirected away from `~/.m2` for every build, not only the ones that publish. The suite runs
     * in parallel forks against a shared Gradle user home, and a test that installed an artefact
     * into a developer's real repository would leak into their next build of an unrelated project.
     */
    val mavenRepository: File get() = rootDirectory.resolve("maven-local")

    /**
     * The directory recording what this fixture has published locally.
     *
     * Redirected for the same reason as [mavenRepository], and additionally because the default
     * lives in the Gradle user home that TestKit shares between tests: one test's publication
     * would otherwise switch local mode on for every other test running at that moment.
     */
    var stateDirectory: File = rootDirectory.resolve("local-state")

    /**
     * Points this fixture at another fixture's local state and Maven repository.
     *
     * This is what makes a consumer build see what a producer build published. The two are
     * separate checkouts in separate directories, exactly as they are in a real workspace, and the
     * only thing they share is the pair of machine level locations the feature is built around.
     *
     * @param producer The fixture whose publications this one should resolve.
     */
    fun resolvingFrom(producer: KreateBuildFixture) {
        sharedLocations(producer.stateDirectory, producer.mavenRepository)
    }

    /**
     * Points this fixture at a state directory and Maven repository shared with other fixtures.
     *
     * @param state The shared state directory.
     * @param maven The shared local Maven repository.
     */
    fun sharedLocations(state: File, maven: File) {
        stateDirectory = state
        sharedMavenRepository = maven
    }

    private var sharedMavenRepository: File? = null

    private val effectiveMavenRepository: File get() = sharedMavenRepository ?: mavenRepository

    /**
     * Writes the settings file. Repositories are declared here rather than relying on the
     * plugin injecting them, matching how an enterprise build is set up.
     */
    fun writeSettings(projectName: String = "sample") {
        write(
            "settings.gradle.kts",
            """
            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                    gradlePluginPortal()
                }
            }

            rootProject.name = "$projectName"
            """.trimIndent()
        )
    }

    /**
     * Writes a build file that applies Kotlin/JVM and Kreate with the given configuration.
     *
     * @param kreateBlock The body of the `kreate { }` block.
     * @param extraPlugins Additional plugin ids applied before Kreate.
     * @param extra Additional build script content appended after the Kreate block.
     */
    fun writeBuild(
        kreateBlock: String,
        extraPlugins: List<String> = emptyList(),
        extra: String = ""
    ) {
        val plugins = buildList {
            add("""id("org.jetbrains.kotlin.jvm")""")
            addAll(extraPlugins)
            add("""id("com.davils.kreate")""")
        }.joinToString("\n    ")

        write(
            "build.gradle.kts",
            """
            plugins {
                $plugins
            }

            group = "com.example"

            kreate {
                $kreateBlock
            }

            $extra
            """.trimIndent()
        )
    }

    /**
     * Writes a build file that applies Kotlin Multiplatform and Kreate with the given configuration.
     *
     * Several of Kreate's defaults are named after configurations and tasks the Kotlin JVM plugin
     * registers, and under the multiplatform plugin those names do not exist. The failure mode is
     * always the same and always quiet: the build stays green and the thing stops happening. Tests
     * covering that need a real multiplatform project rather than a JVM one.
     *
     * JVM and Wasm, deliberately: two targets are enough for a per-target name to be wrong in a way
     * one target would hide, and neither needs an SDK the test machine might not have.
     *
     * @param kreateBlock The body of the `kreate { }` block.
     * @param extraPlugins Additional plugin ids applied before Kreate.
     * @param extra Additional build script content appended after the Kreate block.
     */
    fun writeMultiplatformBuild(
        kreateBlock: String,
        extraPlugins: List<String> = emptyList(),
        extra: String = ""
    ) {
        val plugins = buildList {
            add("""id("org.jetbrains.kotlin.multiplatform")""")
            addAll(extraPlugins)
            add("""id("com.davils.kreate")""")
        }.joinToString("\n    ")

        write(
            "build.gradle.kts",
            """
            @file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

            plugins {
                $plugins
            }

            group = "com.example"

            repositories {
                mavenCentral()
            }

            kotlin {
                jvm()
                wasmJs { nodejs() }
            }

            kreate {
                $kreateBlock
            }

            $extra
            """.trimIndent()
        )
    }

    /**
     * Writes a source file below `src/main/kotlin`.
     *
     * @param relativePath The path below the source root.
     * @param content The file content.
     */
    fun writeKotlin(relativePath: String, content: String) {
        write("src/main/kotlin/$relativePath", content)
    }

    /**
     * Writes a source file below a multiplatform source set.
     *
     * @param sourceSet The source set name, such as `commonMain`.
     * @param relativePath The path below the source root.
     * @param content The file content.
     */
    fun writeKotlin(sourceSet: String, relativePath: String, content: String) {
        write("src/$sourceSet/kotlin/$relativePath", content)
    }

    /**
     * Writes an arbitrary file below the project root, creating parent directories.
     *
     * @param relativePath The path below the project root.
     * @param content The file content.
     * @return The written file.
     */
    fun write(relativePath: String, content: String): File {
        val file = rootDirectory.resolve(relativePath)
        file.parentFile.mkdirs()
        file.writeText(content.trimIndent() + "\n")
        return file
    }

    /**
     * Declares an environment variable for every subsequent build.
     *
     * @param name The variable name.
     * @param value The variable value.
     */
    fun withEnvironment(name: String, value: String) {
        environment[name] = value
    }

    /**
     * Reads a file below the project root.
     *
     * @param relativePath The path below the project root.
     * @return The file, which may not exist.
     */
    fun file(relativePath: String): File = rootDirectory.resolve(relativePath)

    /**
     * Runs Gradle and expects the build to succeed.
     *
     * @param arguments The Gradle command line arguments.
     * @return The build result.
     */
    fun build(vararg arguments: String): BuildResult = runner(arguments.toList()).build()

    /**
     * Runs Gradle and expects the build to succeed, without the Kotlin plugin's own test tasks.
     *
     * For multiplatform builds that reach `check`. The Wasm target's test task unpacks a Node.js
     * and a Yarn distribution into the TestKit Gradle user home, which every functional test
     * shares. The suite runs in parallel forks against that one user home, and two of them
     * unpacking at the same moment is a race Windows loses on a file it cannot replace while it is
     * open — an `UnexpectedBuildFailure` in whichever build happened to be second. No test asserts
     * anything about what the Wasm target runs, so leaving those tasks out costs nothing.
     *
     * A task named on the command line still runs; only what `check` would have pulled in through
     * the aggregate is dropped.
     *
     * @param arguments The Gradle command line arguments.
     * @return The build result.
     */
    fun buildWithoutKotlinTestTasks(vararg arguments: String): BuildResult =
        build(*arguments, "-x", "allTests", "-x", "wasmJsNodeTest")

    /**
     * Runs Gradle and expects the build to fail.
     *
     * @param arguments The Gradle command line arguments.
     * @return The build result.
     */
    fun buildAndFail(vararg arguments: String): BuildResult = runner(arguments.toList()).buildAndFail()

    /**
     * Runs Gradle with additional environment variables and expects the build to succeed.
     *
     * The given entries are merged into the current environment rather than replacing it, because
     * TestKit hands the map straight to the forked process and a build without `PATH` or
     * `JAVA_HOME` does not get far.
     *
     * @param environment The variables to add or override.
     * @param arguments The Gradle command line arguments.
     * @return The build result.
     */
    fun buildWithEnvironment(environment: Map<String, String>, vararg arguments: String): BuildResult =
        runner(arguments.toList())
            .withEnvironment(environmentWith(environment))
            .build()

    /**
     * The environment handed to a build this fixture drives.
     *
     * The CI variables are stripped unless a test asked for them, and that is not tidiness — it is
     * what makes the suite runnable on a CI agent at all. TestKit forks the build with the
     * runner's own environment, so on GitHub Actions every generated build would see
     * `CI=true` and `GITHUB_ACTIONS=true` and conclude, correctly, that it is a pipeline:
     * `kreateLocalPublish` refuses to run there and local mode never activates. Every test of the
     * local development workflow would then fail, on the agent only, for a reason that is the
     * feature working as designed.
     *
     * A test that is *about* CI detection passes the variable explicitly, and it survives this
     * because the caller's entries are applied last.
     *
     * @param extra The variables the test asked for.
     * @return The environment for the forked build.
     */
    private fun environmentWith(extra: Map<String, String>): Map<String, String> =
        System.getenv().filterKeys { key -> key !in CI_VARIABLES } + extra

    private fun runner(arguments: List<String>): GradleRunner = GradleRunner.create()
        .withProjectDir(rootDirectory)
        .withPluginClasspath()
        .withArguments(
            arguments + listOf(
                "--stacktrace",
                "--configuration-cache",
                // Both locations are machine level by design, which is what makes the local
                // development feature work across checkouts — and what would make this suite
                // write into the developer's own Maven repository and Gradle user home if they
                // were not redirected here.
                "-Dmaven.repo.local=${effectiveMavenRepository.absolutePath}",
                "-Pkreate.local.state.dir=${stateDirectory.absolutePath}"
            )
        )
        .forwardOutput()
        .let { runner -> gradleVersion?.let(runner::withGradleVersion) ?: runner }
        // Always set, rather than only when a test asked for variables: on a CI agent the
        // inherited environment is itself the thing that has to be corrected. See
        // [environmentWith].
        .withEnvironment(environmentWith(environment))

    /**
     * Companion object holding shared fixture snippets.
     */
    companion object {
        /**
         * The environment variables Kreate reads as "this is a pipeline".
         *
         * Kept in step with `LocalExtension.ciEnvironmentVariables` and
         * `KreateSettingsExtension.ciEnvironmentVariables`. A variable added there and not here
         * makes the suite fail on an agent that sets it, and nowhere else.
         */
        val CI_VARIABLES: Set<String> =
            setOf("CI", "GITLAB_CI", "GITHUB_ACTIONS", "CI_PIPELINE_ID")

        /**
         * The Java version the generated builds target.
         *
         * Taken from the JVM running the tests so that the fixture never depends on a
         * toolchain that happens not to be installed on the machine or CI agent.
         */
        val javaVersion: Int = Runtime.version().feature()

        /**
         * A `platform { }` block pinned to [javaVersion].
         */
        val platformBlock: String = """
            platform {
                javaVersion = JavaVersion.VERSION_$javaVersion
                explicitApi = false
                allWarningsAsErrors = false
            }
        """.trimIndent()
    }
}
