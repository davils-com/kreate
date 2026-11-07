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

import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.task.FindingCollectTask
import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.dependency.ResolvedGraph
import com.davils.kreate.governance.policy.BuildPolicy
import com.davils.kreate.governance.policy.BuildState
import com.davils.kreate.governance.policy.DEFAULT_TEST_LIBRARIES
import com.davils.kreate.governance.policy.DeclaredDependency
import com.davils.kreate.governance.policy.ModulePolicy
import com.davils.kreate.governance.policy.PolicySettings
import com.davils.kreate.governance.policy.RepositoryDeclaration
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.time.Instant

@CacheableTask
internal abstract class DependencyPolicyCheck : FindingCollectTask(
    "Checks the dependencies, repositories and Gradle settings against the dependency policy.",
    KreateTaskGroup.ASSURANCE
) {
    @get:Input
    public abstract val graphs: ListProperty<ResolvedGraph>

    @get:Input
    public abstract val declared: ListProperty<DeclaredDependency>

    @get:Input
    public abstract val forced: ListProperty<ModuleCoordinate>

    @get:Input
    public abstract val repositories: ListProperty<RepositoryDeclaration>

    @get:Input
    public abstract val denied: MapProperty<String, String>

    @get:Input
    public abstract val allowed: SetProperty<String>

    @get:Input
    public abstract val allowedRepositoryHosts: SetProperty<String>

    @get:Input
    public abstract val forbidDynamicVersions: Property<Boolean>

    @get:Input
    public abstract val forbidChangingVersions: Property<Boolean>

    @get:Input
    public abstract val testLibraries: SetProperty<String>

    @get:Input
    public abstract val requireLocking: Property<Boolean>

    @get:Input
    public abstract val requireVerification: Property<Boolean>

    @get:Input
    public abstract val lockMode: Property<String>

    @get:Input
    public abstract val verificationMode: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract val lockFile: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract val verificationMetadata: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract val wrapperProperties: ConfigurableFileCollection

    @TaskAction
    public fun check() {
        val settings = settings()
        val listed = graphs.get().flatMap { graph -> graph.modules }
        val modules = listed.distinct()
        val moduleFindings = ModulePolicy(settings).findingsOf(modules, declared.get(), forced.get())
        val buildFindings = BuildPolicy(settings).findingsOf(repositories.get(), stateOf(modules))
        val findings = moduleFindings + buildFindings
        writeDocument(FindingDocument(FindingSource.DEPENDENCIES, DocumentStatus.COMPLETE, Instant.now(), findings))
        logger.lifecycle("Checked the dependency policy: ${findings.size} finding(s).")
        findings.forEach { finding -> logger.lifecycle(describe(finding)) }
    }

    private fun settings(): PolicySettings = PolicySettings(
        denied = denied.get(),
        allowed = allowed.get(),
        allowedRepositoryHosts = allowedRepositoryHosts.get(),
        forbidDynamicVersions = forbidDynamicVersions.get(),
        forbidChangingVersions = forbidChangingVersions.get(),
        testLibraries = DEFAULT_TEST_LIBRARIES + testLibraries.get(),
        requireLocking = requireLocking.get(),
        requireVerification = requireVerification.get()
    )

    private fun stateOf(modules: List<ModuleCoordinate>): BuildState = BuildState(
        hasExternalModules = modules.isNotEmpty(),
        lockFilePresent = lockFile.any { file -> file.isFile },
        lockMode = lockMode.get(),
        verificationMode = verificationMode.get(),
        verificationMetadata = textOf(verificationMetadata),
        wrapperProperties = textOf(wrapperProperties)
    )

    private fun textOf(files: ConfigurableFileCollection): String? {
        val file = files.firstOrNull { candidate -> candidate.isFile } ?: return null
        return file.readText()
    }

    private fun describe(finding: Finding): String {
        val subject = finding.module ?: finding.location?.path.orEmpty()
        return "${finding.severity.name} ${finding.ruleId} $subject: ${finding.title}"
    }
}
