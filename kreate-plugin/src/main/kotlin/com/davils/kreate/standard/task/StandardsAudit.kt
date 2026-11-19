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
package com.davils.kreate.standard.task

import com.davils.kreate.KREATE_VERSION
import com.davils.kreate.assurance.finding.AssuranceInputException
import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.task.FindingCollectTask
import com.davils.kreate.net.HttpTransport
import com.davils.kreate.net.HttpTransports
import com.davils.kreate.net.NetworkSettings
import com.davils.kreate.net.ProxySettings
import com.davils.kreate.standard.drift.AuditResult
import com.davils.kreate.standard.drift.StandardsAuditor
import com.davils.kreate.standard.fetch.RevalidatingCache
import com.davils.kreate.standard.lock.SnapshotsJson
import com.davils.kreate.standard.lock.StandardsLock
import com.davils.kreate.standard.lock.StandardsLockJson
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.provider.ProviderContext
import com.davils.kreate.standard.provider.ServiceEndpoints
import com.davils.kreate.standard.provider.StandardProviders
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.time.Clock
import java.time.Duration
import java.time.Instant

private val TIME_TO_LIVE: Duration = Duration.ofHours(24)

private val BULK_TIMEOUT: Duration = Duration.ofMinutes(10)

@DisableCachingByDefault(because = "Standards change without any input of the build changing")
internal abstract class StandardsAudit : FindingCollectTask(
    "Compares the reviewed state of every declared standard with what its body publishes today.",
    KreateTaskGroup.ASSURANCE
) {
    @get:Input
    public abstract val declarations: ListProperty<StandardDeclaration>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val lockFile: ConfigurableFileCollection

    @get:Input
    public abstract val lockPath: Property<String>

    @get:Input
    public abstract val endpoints: MapProperty<String, String>

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

    @get:Internal
    public abstract val githubToken: Property<String>

    @get:OutputFile
    public abstract val snapshotsFile: RegularFileProperty

    @TaskAction
    public fun audit() {
        val lock = try {
            readLock()
        } catch (cause: AssuranceInputException) {
            val reason = cause.message.orEmpty()
            writeResult(AuditResult(incompleteDocument(reason), emptyMap()))
            return
        }
        val transport = lazy { transport(NetworkSettings.DEFAULT_TIMEOUT) }
        val bulkTransport = lazy { transport(BULK_TIMEOUT) }
        try {
            val result = auditor(transport, bulkTransport).audit(declarations.get(), lock, offline.get())
            writeResult(result)
        } finally {
            if (transport.isInitialized()) transport.value.close()
            if (bulkTransport.isInitialized()) bulkTransport.value.close()
        }
    }

    private fun auditor(transport: Lazy<HttpTransport>, bulkTransport: Lazy<HttpTransport>): StandardsAuditor {
        val clock = Clock.systemUTC()
        val cache = RevalidatingCache(cacheDirectory.get().asFile.toPath(), TIME_TO_LIVE, clock)
        val serviceEndpoints = ServiceEndpoints(endpoints.get())
        val token = githubToken.orNull?.takeIf { value -> value.isNotBlank() }
        val context = ProviderContext(serviceEndpoints, cache, { transport.value }, { bulkTransport.value }, token)
        return StandardsAuditor({ StandardProviders.all(context) }, { cache.oldestAnswer }, clock)
    }

    private fun readLock(): StandardsLock {
        val file = lockFile.files.firstOrNull { candidate -> candidate.isFile } ?: return StandardsLock()
        return StandardsLockJson.parse(file.readText(), lockPath.get())
    }

    private fun writeResult(result: AuditResult) {
        writeDocument(result.document)
        val snapshots = snapshotsFile.get().asFile
        snapshots.parentFile.mkdirs()
        snapshots.writeText(SnapshotsJson.render(result.snapshots))
        val document = result.document
        logger.lifecycle("Audited ${declarations.get().size} standard(s): ${document.findings.size} finding(s).")
        document.findings.forEach { finding ->
            logger.lifecycle("${finding.severity.name} ${finding.ruleId}: ${finding.title}")
        }
        document.reason?.let { reason -> logger.warn("The audit of the standards is incomplete: $reason") }
    }

    private fun incompleteDocument(reason: String): FindingDocument =
        FindingDocument(FindingSource.STANDARDS, DocumentStatus.INCOMPLETE, Instant.now(), reason = reason)

    private fun transport(timeout: Duration): HttpTransport {
        val proxy = ProxySettings(httpsProxy.orNull, noProxy.orNull)
        val settings = NetworkSettings("kreate/$KREATE_VERSION", timeout = timeout, proxy = proxy)
        return HttpTransports.create(settings, Clock.systemUTC())
    }
}
