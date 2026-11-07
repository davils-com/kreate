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

import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.inspection.task.InspectedArtifact
import com.davils.kreate.inspection.task.ResolvedPoms
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ArtifactResult
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.UnresolvedArtifactResult
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact

internal fun Project.resolvedPomsOf(modules: List<ModuleCoordinate>): ResolvedPoms {
    val query = dependencies.createArtifactResolutionQuery()
    modules.forEach { module -> query.forModule(module.group, module.name, module.version) }
    val result = query.withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java).execute()
    val poms = result.resolvedComponents.flatMap { component -> component.getArtifacts(MavenPomArtifact::class.java) }
    val outcomes = poms.map { pom -> outcomeOf(pom) }
    val artifacts = outcomes.flatMap { outcome -> outcome.artifacts }
    val unresolved = result.components - result.resolvedComponents
    val failures = outcomes.flatMap { outcome -> outcome.failures }
    return ResolvedPoms(artifacts, failures + unresolved.map { component -> component.id.displayName })
}

private fun outcomeOf(pom: ArtifactResult): ResolvedPoms {
    if (pom is UnresolvedArtifactResult) return ResolvedPoms(emptyList(), listOf(pom.failure.message.orEmpty()))
    val resolved = pom as? ResolvedArtifactResult ?: return ResolvedPoms(emptyList(), emptyList())
    val identifier = resolved.id.componentIdentifier as? ModuleComponentIdentifier
        ?: return ResolvedPoms(emptyList(), emptyList())
    val coordinate = ModuleCoordinate(identifier.group, identifier.module, identifier.version)
    return ResolvedPoms(listOf(InspectedArtifact(coordinate, resolved.file)), emptyList())
}
