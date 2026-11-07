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
package com.davils.kreate.policy.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.task.CurrentDateSource
import com.davils.kreate.assurance.task.IgnoreRuleInput
import com.davils.kreate.assurance.wiring.configValues
import com.davils.kreate.dependency.ResolvedGraph
import com.davils.kreate.dependency.wiring.resolvedGraphOf
import com.davils.kreate.policy.PolicyTaskNames
import com.davils.kreate.policy.model.AssurancePolicyJson
import com.davils.kreate.policy.task.AssurancePolicyCheck
import com.davils.kreate.policy.task.AssurancePolicyTemplate
import com.davils.kreate.trivy.wiring.productionConfigurations
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.register

internal const val POLICY_PROPERTY: String = "kreate.assurance.policy"

private const val DOCUMENT_FILE: String = "kreate/assurance/documents/policy.json"

private const val TEMPLATE_FILE: String = "kreate/assurance/policy-template.json"

private const val JSON_EXTENSION: String = ".json"

internal fun Project.policyNotation(): Provider<String> = providers.gradleProperty(POLICY_PROPERTY)

internal fun Project.registerPolicyTasks(extension: KreateExtension, rules: List<IgnoreRuleInput>) {
    val assurance = extension.assurance
    val owner = this
    val values = provider { configValues(assurance, extension.trivy) }

    tasks.register<AssurancePolicyTemplate>(PolicyTaskNames.TEMPLATE) {
        policyName.convention(owner.provider { "${owner.isolated.rootProject.name}-policy" })
        this.values.set(values)
        forbidIgnoreOf.set(assurance.forbidIgnoreOf)
        outputFile.set(owner.layout.buildDirectory.file(TEMPLATE_FILE))
    }

    val notation = policyNotation().orNull ?: return
    val source = policySource(notation)
    tasks.register<AssurancePolicyCheck>(PolicyTaskNames.CHECK) {
        this.notation.set(notation)
        policyFiles.from(source.files)
        failures.set(source.failures)
        this.values.set(values)
        ignoreRules.set(rules)
        graphs.set(owner.productionGraphs())
        today.set(owner.providers.of(CurrentDateSource::class.java) { })
        documentFile.set(owner.layout.buildDirectory.file(DOCUMENT_FILE))
    }
}

internal fun Project.policySeverities(): Provider<Set<Severity>> {
    val notation = policyNotation().orNull ?: return provider { emptySet() }
    val source = policySource(notation)
    return provider {
        val file = source.files.files.firstOrNull { candidate -> candidate.isFile }
        file?.let { existing -> AssurancePolicyJson.parse(existing.readText()).forbidIgnoreOf }.orEmpty()
    }
}

private fun Project.policySource(notation: String): PolicySource {
    if (notation.endsWith(JSON_EXTENSION) || notation.startsWith("file:")) {
        val file = isolated.rootProject.projectDirectory.file(notation.removePrefix("file:"))
        return PolicySource(files(file), provider { emptyList() })
    }
    val dependency = dependencies.create("$notation@json")
    val detached = configurations.detachedConfiguration(dependency)
    detached.isTransitive = false
    val view = detached.incoming.artifactView { lenient(true) }
    val artifacts: ArtifactCollection = view.artifacts
    val failures = provider { artifacts.failures.map { failure -> failure.message.orEmpty() } }
    return PolicySource(artifacts.artifactFiles, failures)
}

private fun Project.productionGraphs(): ListProperty<ResolvedGraph> {
    val merged = objects.listProperty(ResolvedGraph::class.java)
    val names = productionConfigurations()
    val named = configurations.filter { configuration -> configuration.name in names }
    val resolvable = named.filter { configuration -> configuration.isCanBeResolved }
    resolvable.forEach { configuration ->
        val root = configuration.incoming.resolutionResult.rootComponent
        merged.add(root.map(::resolvedGraphOf))
    }
    return merged
}
