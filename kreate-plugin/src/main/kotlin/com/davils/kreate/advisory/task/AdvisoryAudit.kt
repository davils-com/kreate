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
package com.davils.kreate.advisory.task

import com.davils.kreate.KREATE_VERSION
import com.davils.kreate.advisory.AdvisoryAuditor
import com.davils.kreate.advisory.osv.OsvCache
import com.davils.kreate.advisory.osv.OsvClient
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.task.FindingCollectTask
import com.davils.kreate.dependency.ResolvedGraph
import com.davils.kreate.net.HttpTransport
import com.davils.kreate.net.HttpTransports
import com.davils.kreate.net.NetworkSettings
import com.davils.kreate.net.ProxySettings
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

@DisableCachingByDefault(because = "Advisories change without any input of the build changing")
internal abstract class AdvisoryAudit :
    FindingCollectTask("Audits the resolved dependencies against OSV.dev.", KreateTaskGroup.ASSURANCE) {
    @get:Input
    public abstract val graphs: ListProperty<ResolvedGraph>

    @get:Input
    public abstract val internalGroups: SetProperty<String>

    @get:Input
    public abstract val osvUrl: Property<String>

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
    public fun audit() {
        val transport = lazy(::transport)
        try {
            val auditor = AdvisoryAuditor({ client(transport.value) }, internalGroups.get(), Clock.systemUTC())
            val document = auditor.audit(graphs.get(), offline.get())
            writeDocument(document)
            logger.lifecycle("Audited ${auditedModules()} module(s) against OSV.dev.")
            document.findings.forEach { finding -> logger.lifecycle(describe(finding)) }
            document.reason?.let { reason -> logger.warn("The advisory audit is incomplete: $reason") }
        } finally {
            if (transport.isInitialized()) transport.value.close()
        }
    }

    private fun auditedModules(): Int {
        val modules = graphs.get().flatMap { graph -> graph.modules }
        return modules.distinct().size
    }

    private fun transport(): HttpTransport {
        val proxy = ProxySettings(httpsProxy.orNull, noProxy.orNull)
        val settings = NetworkSettings("kreate/$KREATE_VERSION", proxy = proxy)
        return HttpTransports.create(settings, Clock.systemUTC())
    }

    private fun client(transport: HttpTransport): OsvClient =
        OsvClient(transport, osvUrl.get(), OsvCache(cacheDirectory.get().asFile.toPath()))

    private fun describe(finding: Finding): String {
        val fix = finding.fixedVersion?.let { fixed -> ", fixed in $fixed" }.orEmpty()
        return "${finding.severity.name} ${finding.ruleId} ${finding.module}:${finding.version}$fix: ${finding.title}"
    }
}
