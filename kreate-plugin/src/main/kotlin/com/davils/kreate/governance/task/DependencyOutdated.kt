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
package com.davils.kreate.governance.task

import com.davils.kreate.KREATE_VERSION
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.task.FindingCollectTask
import com.davils.kreate.governance.OutdatedAuditor
import com.davils.kreate.governance.outdated.OutdatedModule
import com.davils.kreate.governance.outdated.OutdatedSettings
import com.davils.kreate.governance.policy.DeclaredDependency
import com.davils.kreate.net.HttpTransport
import com.davils.kreate.net.HttpTransports
import com.davils.kreate.net.NetworkSettings
import com.davils.kreate.net.ProxySettings
import com.davils.kreate.provenance.depsdev.DepsDevCache
import com.davils.kreate.provenance.depsdev.DepsDevClient
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.time.Clock
import java.time.Duration
import java.util.Locale

private val CACHE_TIME_TO_LIVE: Duration = Duration.ofHours(24)

@DisableCachingByDefault(because = "New releases appear without any input of the build changing")
internal abstract class DependencyOutdated : FindingCollectTask(
    "Measures how far the direct dependencies lag behind their latest releases.",
    KreateTaskGroup.ASSURANCE
) {
    @get:Input
    public abstract val declared: ListProperty<DeclaredDependency>

    @get:Input
    public abstract val internalGroups: SetProperty<String>

    @get:Input
    @get:Optional
    public abstract val maxMajorBehind: Property<Int>

    @get:Input
    @get:Optional
    public abstract val maxLibyears: Property<Double>

    @get:Input
    public abstract val depsDevUrl: Property<String>

    @get:Input
    public abstract val reuseWindow: Property<Long>

    @get:Internal
    public abstract val offline: Property<Boolean>

    @get:Internal
    public abstract val cacheDirectory: DirectoryProperty

    @get:Internal
    public abstract val httpsProxy: Property<String>

    @get:Internal
    public abstract val noProxy: Property<String>

    @TaskAction
    public fun measure() {
        val transport = lazy(::transport)
        try {
            val settings = OutdatedSettings(maxMajorBehind.orNull, maxLibyears.orNull)
            val groups = internalGroups.get()
            val auditor = OutdatedAuditor({ client(transport.value) }, groups, settings, Clock.systemUTC())
            val direct = declared.get().map { dependency -> dependency.coordinate }
            val (document, modules) = auditor.audit(direct, offline.get())
            writeDocument(document)
            logger.lifecycle("${modules.size} of ${direct.distinct().size} direct dependencies have a newer release.")
            modules.forEach { module -> logger.lifecycle(describe(module)) }
            document.findings.forEach { finding -> logger.lifecycle(describe(finding)) }
            document.reason?.let { reason -> logger.warn("The outdated check is incomplete: $reason") }
        } finally {
            if (transport.isInitialized()) transport.value.close()
        }
    }

    private fun describe(finding: Finding): String = "${finding.severity.name} ${finding.ruleId}: ${finding.title}"

    private fun describe(module: OutdatedModule): String {
        val years = module.libyears?.let { value -> String.format(Locale.ROOT, ", %.1f libyears", value) }.orEmpty()
        val coordinate = module.module
        return "  ${coordinate.module} ${coordinate.version} -> ${module.latest} (${module.majorsBehind} major$years)"
    }

    private fun transport(): HttpTransport {
        val proxy = ProxySettings(httpsProxy.orNull, noProxy.orNull)
        val settings = NetworkSettings("kreate/$KREATE_VERSION", proxy = proxy)
        return HttpTransports.create(settings, Clock.systemUTC())
    }

    private fun client(transport: HttpTransport): DepsDevClient {
        val cache = DepsDevCache(cacheDirectory.get().asFile.toPath(), CACHE_TIME_TO_LIVE, Clock.systemUTC())
        return DepsDevClient(transport, depsDevUrl.get(), cache)
    }
}
