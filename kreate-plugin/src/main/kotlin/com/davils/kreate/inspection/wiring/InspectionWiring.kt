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
package com.davils.kreate.inspection.wiring

import com.davils.kreate.assurance.AssuranceInspectionExtension
import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.dependency.wiring.resolvedGraphOf
import com.davils.kreate.inspection.InspectionTaskNames
import com.davils.kreate.inspection.task.DependencyInspect
import com.davils.kreate.inspection.task.InspectedArtifact
import com.davils.kreate.inspection.task.JarInspectionTransform
import com.davils.kreate.trivy.wiring.productionConfigurations
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.provider.ListProperty
import org.gradle.kotlin.dsl.register

private const val INSPECTION_ARTIFACT_TYPE: String = "kreate-inspection"

private const val DOCUMENT_FILE: String = "kreate/assurance/documents/inspection.json"

internal fun Project.registerDependencyInspection(inspection: AssuranceInspectionExtension) {
    inspection.configurations.convention(provider { productionConfigurations() })
    dependencies.registerTransform(JarInspectionTransform::class.java) {
        from.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
        to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, INSPECTION_ARTIFACT_TYPE)
    }

    val inspectedProject = this
    tasks.register<DependencyInspect>(InspectionTaskNames.INSPECT) {
        val collections = inspectedProject.provider { inspectedProject.inspectionArtifacts(inspection) }
        reports.from(collections.map { artifacts -> artifacts.map { collection -> collection.artifactFiles } })
        artifacts.set(collections.flatMap { collection -> inspectedProject.artifactsOf(collection) })
        poms.set(provider { inspectedProject.resolvedPomsOf(inspectedProject.inspectedModules(inspection)) })
        poms.finalizeValueOnRead()
        pomFiles.from(poms.map { resolved -> resolved.artifacts.map { pom -> pom.report } })
        val artifactFailures = collections.map { collection -> failuresOf(collection) }
        failures.set(artifactFailures.zip(poms) { artifacts, resolved -> artifacts + resolved.failures })
        coordinates.set(artifacts.map { entries -> entries.map { entry -> entry.coordinate.toString() }.sorted() })
        failScore.set(inspection.failScore)
        failOnRisk.set(inspection.failOnRisk)
        trustedHosts.set(inspection.trustedHosts)
        documentFile.set(layout.buildDirectory.file(DOCUMENT_FILE))
    }
}

private fun Project.inspectionArtifacts(inspection: AssuranceInspectionExtension): List<ArtifactCollection> {
    val names = inspection.configurations.get()
    val selected = configurations.filter { configuration -> configuration.name in names }
    val resolvable = selected.filter { configuration -> configuration.isCanBeResolved }
    return resolvable.map { configuration ->
        val view = configuration.incoming.artifactView {
            lenient(true)
            componentFilter(::isExternal)
            attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, INSPECTION_ARTIFACT_TYPE)
        }
        view.artifacts
    }
}

private fun Project.inspectedModules(inspection: AssuranceInspectionExtension): List<ModuleCoordinate> {
    val names = inspection.configurations.get()
    val selected = configurations.filter { configuration -> configuration.name in names }
    val resolvable = selected.filter { configuration -> configuration.isCanBeResolved }
    val roots = resolvable.map { configuration -> configuration.incoming.resolutionResult.rootComponent.get() }
    return roots.flatMap { root -> resolvedGraphOf(root).modules }.distinct()
}

private fun isExternal(identifier: ComponentIdentifier): Boolean = identifier is ModuleComponentIdentifier

private fun Project.artifactsOf(collections: List<ArtifactCollection>): ListProperty<InspectedArtifact> {
    val merged = objects.listProperty(InspectedArtifact::class.java)
    collections.forEach { collection -> merged.addAll(collection.resolvedArtifacts.map(::inspectedArtifactsOf)) }
    return merged
}

private fun inspectedArtifactsOf(results: Set<ResolvedArtifactResult>): List<InspectedArtifact> =
    results.mapNotNull { result -> inspectedArtifactOf(result) }

private fun inspectedArtifactOf(result: ResolvedArtifactResult): InspectedArtifact? {
    val identifier = result.id.componentIdentifier as? ModuleComponentIdentifier ?: return null
    val coordinate = ModuleCoordinate(identifier.group, identifier.module, identifier.version)
    return InspectedArtifact(coordinate, result.file)
}

private fun failuresOf(collections: List<ArtifactCollection>): List<String> =
    collections.flatMap { collection -> collection.failures.map { failure -> failure.message.orEmpty() } }
