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
package com.davils.kreate.trivy.task

import com.davils.kreate.KREATE_VERSION
import com.davils.kreate.host.HostPlatform
import com.davils.kreate.net.HttpTransports
import com.davils.kreate.net.NetworkSettings
import com.davils.kreate.net.ProxySettings
import com.davils.kreate.tooling.ExecutableResolver
import com.davils.kreate.tooling.ExternalTool
import com.davils.kreate.tooling.ToolInvocation
import com.davils.kreate.tooling.ToolNotFoundException
import com.davils.kreate.tooling.ToolRunner
import com.davils.kreate.trivy.provision.TrivyAsset
import com.davils.kreate.trivy.provision.TrivyAssets
import com.davils.kreate.trivy.provision.TrivyInstaller
import com.davils.kreate.trivy.provision.TrivyProvisionSettings
import com.davils.kreate.trivy.provision.TrivyProvisioner
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import java.io.File
import java.time.Clock
import java.time.Duration

private val PROBE_TIMEOUT: Duration = Duration.ofSeconds(30)

private val DOWNLOAD_TIMEOUT: Duration = Duration.ofMinutes(10)

internal abstract class TrivyToolInput {
    @get:Input
    public abstract val version: Property<String>

    @get:Input
    public abstract val useInstalled: Property<Boolean>

    @get:Input
    @get:Optional
    public abstract val executable: Property<String>

    @get:Input
    public abstract val checksums: MapProperty<String, String>

    @get:Input
    public abstract val downloadBaseUrl: Property<String>

    @get:Internal
    public abstract val cacheRoot: DirectoryProperty

    @get:Internal
    public abstract val offline: Property<Boolean>

    @get:Internal
    public abstract val httpsProxy: Property<String>

    @get:Internal
    public abstract val noProxy: Property<String>

    fun provisioner(runner: ToolRunner, workingDirectory: File, logger: Logger): TrivyProvisioner = TrivyProvisioner(
        settings = settings(),
        installed = ::installedTrivy,
        versionOf = { command -> versionOf(command, runner, workingDirectory) },
        installer = { installer(logger) },
        warn = logger::warn
    )

    private fun settings(): TrivyProvisionSettings = TrivyProvisionSettings(
        version = version.get(),
        useInstalled = useInstalled.get(),
        executable = executable.orNull,
        checksums = checksums.get(),
        cacheRoot = cacheRoot.get().asFile.toPath(),
        offline = offline.get(),
        asset = hostAsset()
    )

    private fun hostAsset(): TrivyAsset? {
        val host = HostPlatform.current()
        val operatingSystem = host.operatingSystemOrNull() ?: return null
        val architecture = host.architectureOrNull() ?: return null
        return TrivyAssets.forHost(operatingSystem, architecture)
    }

    private fun installedTrivy(): String? = try {
        ExecutableResolver.resolve(ExternalTool.TRIVY)
    } catch (_: ToolNotFoundException) {
        null
    }

    private fun versionOf(command: String, runner: ToolRunner, workingDirectory: File): String? {
        val arguments = listOf(command, "--version", "--format", "json")
        val result = runner.run(ToolInvocation(arguments, workingDirectory, timeout = PROBE_TIMEOUT))
        return try {
            val report = Json.parseToJsonElement(result.stdout.text) as? JsonObject
            (report?.get("Version") as? JsonPrimitive)?.content
        } catch (_: SerializationException) {
            null
        }
    }

    private fun installer(logger: Logger): TrivyInstaller {
        val proxy = ProxySettings(httpsProxy.orNull, noProxy.orNull)
        val network = NetworkSettings("kreate/$KREATE_VERSION", timeout = DOWNLOAD_TIMEOUT, proxy = proxy)
        val transport = HttpTransports.create(network, Clock.systemUTC())
        return TrivyInstaller(transport, downloadBaseUrl.get(), logger::warn)
    }
}
