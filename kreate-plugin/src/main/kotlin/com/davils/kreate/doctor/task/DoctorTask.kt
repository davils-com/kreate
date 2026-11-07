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
package com.davils.kreate.doctor.task

import com.davils.kreate.KREATE_VERSION
import com.davils.kreate.doctor.check.Doctor
import com.davils.kreate.doctor.check.DoctorCheck
import com.davils.kreate.doctor.check.DoctorFacts
import com.davils.kreate.doctor.check.DoctorStatus
import com.davils.kreate.doctor.check.ProxyFacts
import com.davils.kreate.doctor.check.TrivyFacts
import com.davils.kreate.net.HttpMethod
import com.davils.kreate.net.HttpTransport
import com.davils.kreate.net.HttpTransports
import com.davils.kreate.net.NetworkException
import com.davils.kreate.net.NetworkSettings
import com.davils.kreate.net.ProxySettings
import com.davils.kreate.net.TransportRequest
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import com.davils.kreate.tooling.ExecOperationsToolRunner
import com.davils.kreate.tooling.ExecutableResolver
import com.davils.kreate.tooling.ExternalTool
import com.davils.kreate.tooling.ToolNotFoundException
import com.davils.kreate.vcs.GitHistory
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

private val PROBE_TIMEOUT: Duration = Duration.ofSeconds(10)

private const val MAX_PROBE_BYTES: Int = 64 * 1024

@UntrackedTask(because = "It checks the machine it runs on")
internal abstract class DoctorTask @Inject constructor(private val exec: ExecOperations) : KreateTask(
    "Checks the machine Kreate runs on: Java, Gradle, Git, Trivy, the proxy, the network and the cache.",
    KreateTaskGroup.KREATE
) {
    @get:Internal
    public abstract val gradleVersion: Property<String>

    @get:Internal
    public abstract val projectDirectory: DirectoryProperty

    @get:Internal
    public abstract val trivyEnabled: Property<Boolean>

    @get:Internal
    public abstract val trivyVersion: Property<String>

    @get:Internal
    public abstract val trivyUseInstalled: Property<Boolean>

    @get:Internal
    public abstract val cacheDirectory: DirectoryProperty

    @get:Internal
    public abstract val httpsProxy: Property<String>

    @get:Internal
    public abstract val noProxy: Property<String>

    @get:Internal
    public abstract val proxyHost: Property<String>

    @get:Internal
    public abstract val proxyPort: Property<String>

    @get:Internal
    public abstract val offline: Property<Boolean>

    @get:Internal
    public abstract val endpoints: MapProperty<String, String>

    @TaskAction
    public fun examine() {
        val (reachable, unreachable) = probe()
        val facts = DoctorFacts(
            javaVersion = Runtime.version().feature(),
            gradleVersion = gradleVersion.get(),
            gitVersion = gitVersion(),
            trivy = trivyFacts(),
            proxy = ProxyFacts(httpsProxy.orNull, proxyHost.orNull, proxyPort.orNull),
            unreachable = unreachable,
            reachable = reachable,
            offline = offline.get(),
            cacheProblem = cacheProblem()
        )
        val checks = Doctor.checks(facts)
        val width = checks.maxOf { check -> check.name.length }
        checks.forEach { check -> logger.lifecycle(lineOf(check, width)) }
        val failed = checks.filter { check -> check.status == DoctorStatus.FAIL }
        val names = failed.joinToString { check -> check.name }
        if (failed.isNotEmpty()) throw GradleException("${failed.size} check(s) failed: $names.")
    }

    private fun lineOf(check: DoctorCheck, width: Int): String {
        val status = check.status.name
        return "${status.padEnd(STATUS_WIDTH)} ${check.name.padEnd(width)}  ${check.message}"
    }

    private fun gitVersion(): String? = try {
        val git = ExecutableResolver.resolve(ExternalTool.GIT)
        GitHistory(ExecOperationsToolRunner(exec), git, projectDirectory.get().asFile).version()
    } catch (_: ToolNotFoundException) {
        null
    }

    private fun trivyFacts(): TrivyFacts? {
        if (!trivyEnabled.get()) return null
        val version = trivyVersion.get()
        val cached = cacheDirectory.get().asFile.resolve("trivy/$version").isDirectory
        return TrivyFacts(version, trivyUseInstalled.get(), cached, installedTrivy())
    }

    private fun installedTrivy(): String? = try {
        val trivy = ExecutableResolver.resolve(ExternalTool.TRIVY)
        val output = ByteArrayOutputStream()
        exec.exec {
            commandLine(trivy, "--version")
            standardOutput = output
            isIgnoreExitValue = true
        }
        val text = output.toString(Charsets.UTF_8)
        val first = text.lineSequence().firstOrNull()
        first?.trim()
    } catch (_: ToolNotFoundException) {
        null
    }

    private fun probe(): Pair<List<String>, Map<String, String>> {
        if (offline.get()) return emptyList<String>() to emptyMap()
        val proxy = ProxySettings(httpsProxy.orNull, noProxy.orNull)
        val settings = NetworkSettings("kreate/$KREATE_VERSION", timeout = PROBE_TIMEOUT, proxy = proxy)
        val reachable = mutableListOf<String>()
        val unreachable = mutableMapOf<String, String>()
        HttpTransports.create(settings, Clock.systemUTC()).use { transport ->
            endpoints.get().forEach { (name, url) ->
                val problem = problemOf(transport, url)
                if (problem == null) reachable += name
                if (problem != null) unreachable[name] = problem
            }
        }
        return reachable to unreachable
    }

    private fun problemOf(transport: HttpTransport, url: String): String? {
        val uri = URI(url)
        val hosts = setOfNotNull(uri.host)
        val request = TransportRequest(HttpMethod.GET, uri, maxBytes = MAX_PROBE_BYTES, allowedHosts = hosts)
        return try {
            transport.send(request)
            null
        } catch (cause: NetworkException) {
            "$url is not reachable: ${cause.message.orEmpty()} Check the proxy and the network."
        }
    }

    private fun cacheProblem(): String? {
        val directory = cacheDirectory.get().asFile
        return try {
            directory.mkdirs()
            val probe = Files.createTempFile(directory.toPath(), ".doctor", ".tmp")
            Files.delete(probe)
            null
        } catch (cause: IOException) {
            "${directory.path} is not writable: ${cause.message.orEmpty()}"
        }
    }

    private companion object {
        const val STATUS_WIDTH: Int = 5
    }
}
