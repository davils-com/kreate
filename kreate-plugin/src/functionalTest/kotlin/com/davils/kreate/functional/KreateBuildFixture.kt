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
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile

class KreateBuildFixture(
    val rootDirectory: File,
    private val gradleVersion: String? = null
) {

    private val environment: MutableMap<String, String> = mutableMapOf()

    private var sharedMavenRepository: File? = null

    val nativeProjectDirectory: File get() = rootDirectory.resolve("jni/sample")

    val mavenRepository: File get() = rootDirectory.resolve("maven-local")

    var stateDirectory: File = rootDirectory.resolve("local-state")

    private val effectiveMavenRepository: File get() = sharedMavenRepository ?: mavenRepository

    fun resolvingFrom(producer: KreateBuildFixture) {
        sharedLocations(producer.stateDirectory, producer.mavenRepository)
    }

    fun sharedLocations(state: File, maven: File) {
        stateDirectory = state
        sharedMavenRepository = maven
    }

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

    fun writeBuild(
        kreateBlock: String,
        extraPlugins: List<String> = emptyList(),
        extra: String = ""
    ) {
        val plugins = pluginBlock("org.jetbrains.kotlin.jvm", extraPlugins)

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

    fun writeMultiplatformBuild(
        kreateBlock: String,
        extraPlugins: List<String> = emptyList(),
        extra: String = ""
    ) {
        val plugins = pluginBlock("org.jetbrains.kotlin.multiplatform", extraPlugins)

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

    fun writeKotlin(relativePath: String, content: String) {
        write("src/main/kotlin/$relativePath", content)
    }

    fun writeKotlin(sourceSet: String, relativePath: String, content: String) {
        write("src/$sourceSet/kotlin/$relativePath", content)
    }

    fun write(relativePath: String, content: String): File {
        val file = rootDirectory.resolve(relativePath)
        file.parentFile.mkdirs()
        val normalized = content.trimIndent()
        file.writeText(normalized + "\n")
        return file
    }

    fun withEnvironment(name: String, value: String) {
        environment[name] = value
    }

    fun file(relativePath: String): File = rootDirectory.resolve(relativePath)

    fun build(vararg arguments: String): BuildResult {
        val runner = runner(arguments.toList())
        return runner.build()
    }

    fun buildWithoutKotlinTestTasks(vararg arguments: String): BuildResult =
        build(*arguments, "-x", "allTests", "-x", "wasmJsNodeTest")

    fun buildAndFail(vararg arguments: String): BuildResult {
        val runner = runner(arguments.toList())
        return runner.buildAndFail()
    }

    fun buildWithEnvironment(extraEnvironment: Map<String, String>, vararg arguments: String): BuildResult {
        val runner = runner(arguments.toList())
        val combined = environment + extraEnvironment
        val withEnvironment = runner.withEnvironment(environmentWith(combined))
        return withEnvironment.build()
    }

    private fun pluginBlock(kotlinPlugin: String, extraPlugins: List<String>): String {
        val plugins = buildList {
            add("""id("$kotlinPlugin")""")
            addAll(extraPlugins)
            add("""id("com.davils.kreate")""")
        }
        return plugins.joinToString("\n    ")
    }

    private fun environmentWith(extra: Map<String, String>): Map<String, String> {
        val inherited = System.getenv()
        val withoutCi = inherited.filterKeys { key -> key !in CI_VARIABLES }
        return withoutCi + extra
    }

    private fun runner(arguments: List<String>): GradleRunner {
        val isolationArguments = listOf(
            "--stacktrace",
            "--configuration-cache",
            "-Dmaven.repo.local=${effectiveMavenRepository.absolutePath}",
            "-Pkreate.local.state.dir=${stateDirectory.absolutePath}"
        )
        val coverageArguments = currentGradleCoverage()
        val runner = GradleRunner.create()
        runner.withProjectDir(rootDirectory)
        runner.withPluginClasspath()
        runner.withArguments(arguments + isolationArguments + coverageArguments)
        runner.forwardOutput()
        runner.withEnvironment(environmentWith(environment))
        gradleVersion?.let { version -> runner.withGradleVersion(version) }
        return runner
    }

    private fun currentGradleCoverage(): List<String> {
        val isPinnedGradleVersion = gradleVersion != null
        if (isPinnedGradleVersion) return emptyList()
        return coverageJvmArguments()
    }

    companion object {
        val CI_VARIABLES: Set<String> = setOf("CI", "GITLAB_CI", "GITHUB_ACTIONS", "CI_PIPELINE_ID")

        val javaVersion: Int = Runtime.version().feature()

        val platformBlock: String = """
            platform {
                javaVersion = JavaVersion.VERSION_$javaVersion
                explicitApi = false
                allWarningsAsErrors = false
            }
        """.trimIndent()

        fun createIn(workspace: File, gradleVersion: String? = null): KreateBuildFixture {
            val directory = createTempDirectory(workspace.toPath(), "build")
            return KreateBuildFixture(directory.toFile(), gradleVersion)
        }
    }
}

private const val COVERAGE_AGENT_PROPERTY: String = "kreate.test.coverageAgent"

private const val COVERAGE_FILE_PROPERTY: String = "kreate.test.coverageFile"

private const val DAEMON_MEMORY_ARGUMENTS: String = "-Xmx1g -XX:MaxMetaspaceSize=512m"

private const val COVERED_PACKAGES: String = "com.davils.kreate.*"

private fun coverageJvmArguments(): List<String> {
    val agent = System.getProperty(COVERAGE_AGENT_PROPERTY) ?: return emptyList()
    val destination = System.getProperty(COVERAGE_FILE_PROPERTY) ?: return emptyList()
    val agentOptions = "destfile=$destination,append=true,includes=$COVERED_PACKAGES,output=file,dumponexit=false"
    val jvmArguments = "-Dorg.gradle.jvmargs=$DAEMON_MEMORY_ARGUMENTS -javaagent:$agent=$agentOptions"
    val initScript = COVERAGE_DUMP_INIT_SCRIPT.absolutePath
    return listOf(jvmArguments, "--init-script", initScript)
}

private val COVERAGE_DUMP_INIT_SCRIPT: File by lazy { writeCoverageDumpInitScript() }

private fun writeCoverageDumpInitScript(): File {
    val script = createTempFile("kreate-coverage", ".init.gradle")
    val file = script.toFile()
    file.writeText(
        """
        import org.gradle.api.services.BuildService
        import org.gradle.api.services.BuildServiceParameters
        import org.gradle.build.event.BuildEventsListenerRegistry
        import org.gradle.tooling.events.FinishEvent
        import org.gradle.tooling.events.OperationCompletionListener
        import javax.inject.Inject

        abstract class KreateCoverageDump implements BuildService<BuildServiceParameters.None>,
            OperationCompletionListener, AutoCloseable {
            void onFinish(FinishEvent event) {}

            void close() {
                def runtime = Class.forName("org.jacoco.agent.rt.RT", true, ClassLoader.getSystemClassLoader())
                runtime.getMethod("getAgent").invoke(null).dump(false)
            }
        }

        abstract class KreateCoverageDumpPlugin implements Plugin<Settings> {
            @Inject
            abstract BuildEventsListenerRegistry getRegistry()

            void apply(Settings settings) {
                def services = settings.gradle.sharedServices
                def dump = services.registerIfAbsent("kreateCoverageDump", KreateCoverageDump) {}
                registry.onTaskCompletion(dump)
            }
        }

        beforeSettings { settings -> settings.plugins.apply(KreateCoverageDumpPlugin) }
        """.trimIndent()
    )
    file.deleteOnExit()
    return file
}
