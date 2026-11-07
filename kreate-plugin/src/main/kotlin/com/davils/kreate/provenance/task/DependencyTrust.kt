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
package com.davils.kreate.provenance.task

import com.davils.kreate.KREATE_VERSION
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.task.FindingCollectTask
import com.davils.kreate.dependency.ResolvedGraph
import com.davils.kreate.net.HttpTransport
import com.davils.kreate.net.HttpTransports
import com.davils.kreate.net.NetworkSettings
import com.davils.kreate.net.ProxySettings
import com.davils.kreate.provenance.TrustAuditor
import com.davils.kreate.provenance.depsdev.DepsDevCache
import com.davils.kreate.provenance.depsdev.DepsDevClient
import com.davils.kreate.provenance.trust.TrustThresholds
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.time.Clock
import java.time.Duration

private val CACHE_TIME_TO_LIVE: Duration = Duration.ofHours(24)

@DisableCachingByDefault(because = "The age of a dependency changes without any input of the build changing")
internal abstract class DependencyTrust : FindingCollectTask(
    "Assesses the age and maintenance of the dependencies through deps.dev.",
    KreateTaskGroup.ASSURANCE
) {
    @get:Input
    public abstract val graphs: ListProperty<ResolvedGraph>

    @get:Input
    public abstract val internalGroups: SetProperty<String>

    @get:Input
    public abstract val depsDevUrl: Property<String>

    @get:Input
    public abstract val freshPackageDays: Property<Int>

    @get:Input
    public abstract val youngPackageDays: Property<Int>

    @get:Input
    public abstract val freshReleaseHours: Property<Int>

    @get:Input
    public abstract val minScorecard: Property<Double>

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
    public fun assess() {
        val transport = lazy(::transport)
        try {
            val auditor = TrustAuditor(
                client = { client(transport.value) },
                internalGroups = internalGroups.get(),
                thresholds = thresholds(),
                clock = Clock.systemUTC()
            )
            val document = auditor.audit(graphs.get(), offline.get())
            writeDocument(document)
            val modules = graphs.get().flatMap { graph -> graph.modules }
            logger.lifecycle("Assessed ${modules.distinct().size} module(s) through deps.dev.")
            document.findings.forEach { finding -> logger.lifecycle(describe(finding)) }
            document.reason?.let { reason -> logger.warn("The trust assessment is incomplete: $reason") }
        } finally {
            if (transport.isInitialized()) transport.value.close()
        }
    }

    private fun describe(finding: Finding): String {
        val coordinate = "${finding.module.orEmpty()}:${finding.version.orEmpty()}"
        return "${finding.severity.name} ${finding.ruleId} $coordinate: ${finding.title}"
    }

    private fun thresholds(): TrustThresholds = TrustThresholds(
        freshPackageDays = freshPackageDays.get(),
        youngPackageDays = youngPackageDays.get(),
        freshReleaseHours = freshReleaseHours.get(),
        minScorecard = minScorecard.get()
    )

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
