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
import com.davils.kreate.advisory.AdvisoryAuditor
import com.davils.kreate.advisory.osv.OsvCache
import com.davils.kreate.advisory.osv.OsvClient
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.dependency.ResolvedGraph
import com.davils.kreate.governance.add.AddDecision
import com.davils.kreate.governance.add.DependencyAdder
import com.davils.kreate.governance.add.ModuleRequest
import com.davils.kreate.governance.catalog.CatalogNames
import com.davils.kreate.governance.catalog.VersionCatalog
import com.davils.kreate.governance.policy.DEFAULT_TEST_LIBRARIES
import com.davils.kreate.governance.policy.ModulePolicy
import com.davils.kreate.governance.policy.PolicySettings
import com.davils.kreate.net.HttpTransport
import com.davils.kreate.net.HttpTransports
import com.davils.kreate.net.NetworkSettings
import com.davils.kreate.net.ProxySettings
import com.davils.kreate.provenance.TrustAuditor
import com.davils.kreate.provenance.depsdev.DepsDevCache
import com.davils.kreate.provenance.depsdev.DepsDevClient
import com.davils.kreate.provenance.depsdev.DepsDevUnavailableException
import com.davils.kreate.provenance.isInternal
import com.davils.kreate.provenance.typosquat.PopularModules
import com.davils.kreate.provenance.typosquat.TyposquatDetector
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.api.tasks.VerificationException
import org.gradle.api.tasks.options.Option
import java.nio.file.Path
import java.time.Clock
import java.time.Duration

private val CACHE_TIME_TO_LIVE: Duration = Duration.ofHours(24)

@UntrackedTask(because = "It checks a module online and edits the version catalog")
internal abstract class DependencyAdd : KreateTask(
    "Checks a module and adds exactly the checked version to gradle/libs.versions.toml.",
    KreateTaskGroup.ASSURANCE
) {
    @get:Internal
    @get:Option(option = "module", description = "The module, as group:artifact or group:artifact:version.")
    public abstract val module: Property<String>

    @get:Internal
    @get:Option(option = "alias", description = "The alias in the catalog. Defaults to the artifact name.")
    public abstract val alias: Property<String>

    @get:Internal
    @get:Option(option = "force", description = "Adds the module despite blocking findings.")
    public abstract val force: Property<Boolean>

    @get:Internal
    @get:Option(option = "dry-run", description = "Checks the module without changing the catalog.")
    public abstract val dryRun: Property<Boolean>

    @get:Internal
    public abstract val catalogFile: RegularFileProperty

    @get:Internal
    public abstract val internalGroups: SetProperty<String>

    @get:Internal
    public abstract val denied: MapProperty<String, String>

    @get:Internal
    public abstract val allowed: SetProperty<String>

    @get:Internal
    public abstract val popularModules: SetProperty<String>

    @get:Internal
    public abstract val typosquatAllow: SetProperty<String>

    @get:Internal
    public abstract val trustThresholds: Property<TrustThresholdsInput>

    @get:Internal
    public abstract val osvUrl: Property<String>

    @get:Internal
    public abstract val depsDevUrl: Property<String>

    @get:Internal
    public abstract val offline: Property<Boolean>

    @get:Internal
    public abstract val cacheDirectory: DirectoryProperty

    @get:Internal
    public abstract val httpsProxy: Property<String>

    @get:Internal
    public abstract val noProxy: Property<String>

    @TaskAction
    public fun add() {
        val request = requestOf(module.orNull ?: throw InvalidUserDataException("Name the module with --module."))
        val transport = lazy(::transport)
        try {
            val target = ModuleCoordinate(request.group, request.name, versionOf(request) { transport.value })
            val decision = adder { transport.value }.decide(target)
            report(decision)
            enforce(decision)
            if (dryRun.getOrElse(false)) return logger.lifecycle("Dry run: the catalog is unchanged.")
            write(target)
        } finally {
            if (transport.isInitialized()) transport.value.close()
        }
    }

    private fun requestOf(notation: String): ModuleRequest = try {
        ModuleRequest.parse(notation)
    } catch (cause: IllegalArgumentException) {
        throw InvalidUserDataException(cause.message.orEmpty(), cause)
    }

    private fun versionOf(request: ModuleRequest, transport: () -> HttpTransport): String {
        val explicit = request.version
        if (explicit != null) return explicit
        val probe = ModuleCoordinate(request.group, request.name, "")
        if (isInternal(probe, internalGroups.get())) {
            throw InvalidUserDataException("${request.module} is internal and is never sent; name its version.")
        }
        val latest = latestOf(probe, transport)
        if (latest == null || !CatalogNames.isExactVersion(latest)) {
            throw InvalidUserDataException("deps.dev knows no exact release of ${request.module}; name its version.")
        }
        return latest
    }

    private fun latestOf(probe: ModuleCoordinate, transport: () -> HttpTransport): String? {
        if (offline.get()) throw GradleException("The latest version of ${probe.module} cannot be read offline.")
        return try {
            depsDev(transport()).historyOf(probe)?.latest
        } catch (cause: DepsDevUnavailableException) {
            val reason = cause.message.orEmpty()
            throw GradleException("The latest version of ${probe.module} cannot be read: $reason", cause)
        }
    }

    private fun adder(transport: () -> HttpTransport): DependencyAdder {
        val graph = { target: ModuleCoordinate -> listOf(ResolvedGraph(listOf(target), emptyList())) }
        val groups = internalGroups.get()
        val clock = Clock.systemUTC()
        val osvCache = OsvCache(cacheRoot().resolve("osv"))
        val osv = AdvisoryAuditor({ OsvClient(transport(), osvUrl.get(), osvCache) }, groups, clock)
        val trust = TrustAuditor({ depsDev(transport()) }, groups, trustThresholds.get().toThresholds(), clock)
        val typosquat = TyposquatDetector(PopularModules.coordinates + popularModules.get(), typosquatAllow.get())
        val policy = ModulePolicy(PolicySettings(denied.get(), allowed.get(), testLibraries = DEFAULT_TEST_LIBRARIES))
        return DependencyAdder(
            advisories = { target -> osv.audit(graph(target), offline.get()) },
            trust = { target -> trust.audit(graph(target), offline.get()) },
            typosquat = typosquat,
            policy = policy
        )
    }

    private fun report(decision: AddDecision) {
        val target = decision.module
        logger.lifecycle("Checked ${target.module}:${target.version}: ${decision.findings.size} finding(s).")
        decision.findings.forEach { finding -> logger.lifecycle(describe(finding)) }
    }

    private fun enforce(decision: AddDecision) {
        val incomplete = decision.incompleteReason
        if (incomplete != null) {
            val module = decision.module.module
            throw GradleException("$module could not be checked, and --force does not change that: $incomplete")
        }
        if (decision.blockReasons.isEmpty()) return
        val reasons = decision.blockReasons.joinToString("\n  ", prefix = "\n  ")
        if (force.getOrElse(false)) return logger.warn("Adding ${decision.module.module} despite:$reasons")
        throw VerificationException("${decision.module.module} is not added:$reasons\nUse --force to add it anyway.")
    }

    private fun write(target: ModuleCoordinate) {
        val file = catalogFile.get().asFile
        val existingFile = file.takeIf { candidate -> candidate.isFile }
        val text = existingFile?.readText().orEmpty()
        val catalog = VersionCatalog(text)
        val existing = catalog.libraryOf(target.group, target.name)
        if (existing != null) {
            throw InvalidUserDataException("${target.module} is already in the catalog as ${existing.alias}.")
        }
        val name = alias.orNull ?: CatalogNames.aliasFor(target.name)
        val updated = try {
            catalog.withLibrary(name, target.group, target.name, target.version)
        } catch (cause: IllegalArgumentException) {
            throw InvalidUserDataException(cause.message.orEmpty(), cause)
        }
        file.parentFile.mkdirs()
        file.writeText(updated)
        logger.lifecycle("Added $name = ${target.module}:${target.version} to ${file.name}.")
        logger.lifecycle("Declare it with implementation(libs.${CatalogNames.accessorOf(name)}).")
    }

    private fun describe(finding: Finding): String =
        "  ${finding.severity.name} ${finding.ruleId}: ${finding.title}"

    private fun transport(): HttpTransport {
        val proxy = ProxySettings(httpsProxy.orNull, noProxy.orNull)
        val settings = NetworkSettings("kreate/$KREATE_VERSION", proxy = proxy)
        return HttpTransports.create(settings, Clock.systemUTC())
    }

    private fun depsDev(transport: HttpTransport): DepsDevClient {
        val cache = DepsDevCache(cacheRoot().resolve("deps-dev"), CACHE_TIME_TO_LIVE, Clock.systemUTC())
        return DepsDevClient(transport, depsDevUrl.get(), cache)
    }

    private fun cacheRoot(): Path = cacheDirectory.get().asFile.toPath()
}
