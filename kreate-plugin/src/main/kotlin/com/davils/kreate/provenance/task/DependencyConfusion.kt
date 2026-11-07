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
import com.davils.kreate.provenance.ConfusionAuditor
import com.davils.kreate.provenance.confusion.MavenMetadataClient
import com.davils.kreate.provenance.marker.PluginMarker
import com.davils.kreate.provenance.marker.PluginMarkerChecker
import com.davils.kreate.provenance.typosquat.PopularModules
import com.davils.kreate.provenance.typosquat.TyposquatDetector
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.time.Clock

@DisableCachingByDefault(because = "The public repository changes without any input of the build changing")
internal abstract class DependencyConfusion :
    FindingCollectTask("Looks for typosquatting and dependency confusion.", KreateTaskGroup.ASSURANCE) {
    @get:Input
    public abstract val graphs: ListProperty<ResolvedGraph>

    @get:Input
    public abstract val markers: ListProperty<PluginMarker>

    @get:Input
    public abstract val popularModules: SetProperty<String>

    @get:Input
    public abstract val allow: SetProperty<String>

    @get:Input
    public abstract val internalGroups: SetProperty<String>

    @get:Input
    public abstract val checkPublicRepository: Property<Boolean>

    @get:Input
    public abstract val publicRepository: Property<String>

    @get:Input
    public abstract val reuseWindow: Property<Long>

    @get:Internal
    public abstract val offline: Property<Boolean>

    @get:Internal
    public abstract val httpsProxy: Property<String>

    @get:Internal
    public abstract val noProxy: Property<String>

    @TaskAction
    public fun check() {
        val transport = lazy(::transport)
        try {
            val document = auditor(transport).audit(graphs.get(), markers.get(), offline.get())
            writeDocument(document)
            val modules = graphs.get().flatMap { graph -> graph.modules }
            val checked = modules.distinct().size
            logger.lifecycle("Checked $checked module(s) for confusion: ${document.findings.size} finding(s).")
            document.findings.forEach { finding -> logger.lifecycle(describe(finding)) }
            document.reason?.let { reason -> logger.warn("The confusion check is incomplete: $reason") }
        } finally {
            if (transport.isInitialized()) transport.value.close()
        }
    }

    private fun auditor(transport: Lazy<HttpTransport>): ConfusionAuditor {
        val allowed = allow.get()
        val typosquat = TyposquatDetector(PopularModules.coordinates + popularModules.get(), allowed)
        val lookup = { MavenMetadataClient(transport.value, publicRepository.get()) }
        val repository = lookup.takeIf { checkPublicRepository.get() }
        return ConfusionAuditor(
            typosquat = typosquat,
            markers = PluginMarkerChecker(allowed),
            publicRepository = repository,
            internalGroups = internalGroups.get(),
            clock = Clock.systemUTC()
        )
    }

    private fun describe(finding: Finding): String =
        "${finding.severity.name} ${finding.ruleId} ${finding.module.orEmpty()}: ${finding.title}"

    private fun transport(): HttpTransport {
        val proxy = ProxySettings(httpsProxy.orNull, noProxy.orNull)
        val settings = NetworkSettings("kreate/$KREATE_VERSION", proxy = proxy)
        return HttpTransports.create(settings, Clock.systemUTC())
    }
}
