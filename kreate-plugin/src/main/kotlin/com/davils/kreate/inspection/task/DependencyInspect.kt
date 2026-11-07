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
package com.davils.kreate.inspection.task

import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.task.FindingCollectTask
import com.davils.kreate.inspection.InspectedModule
import com.davils.kreate.inspection.InspectionEvaluator
import com.davils.kreate.inspection.InspectionSettings
import com.davils.kreate.inspection.scan.HitsJson
import com.davils.kreate.inspection.scan.PomScanner
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.time.Instant

@CacheableTask
internal abstract class DependencyInspect :
    FindingCollectTask("Inspects the content of the resolved dependencies.", KreateTaskGroup.ASSURANCE) {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract val reports: ConfigurableFileCollection

    @get:Internal
    public abstract val artifacts: ListProperty<InspectedArtifact>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract val pomFiles: ConfigurableFileCollection

    @get:Internal
    public abstract val poms: Property<ResolvedPoms>

    @get:Input
    public abstract val failures: ListProperty<String>

    @get:Input
    public abstract val coordinates: ListProperty<String>

    @get:Input
    public abstract val failScore: Property<Int>

    @get:Input
    public abstract val failOnRisk: Property<Boolean>

    @get:Input
    public abstract val trustedHosts: SetProperty<String>

    @TaskAction
    public fun inspect() {
        val settings = InspectionSettings(failScore.get(), failOnRisk.get(), trustedHosts.get())
        val evaluator = InspectionEvaluator(settings)
        val modules = inspectedModules()
        val findings = modules.flatMap { module -> evaluator.evaluate(module) }
        val problems = failures.get()

        writeDocument(
            FindingDocument(
                source = FindingSource.INSPECTION,
                status = statusOf(problems),
                evaluatedAt = Instant.now(),
                findings = findings,
                reason = problems.firstOrNull()
            )
        )
        logger.lifecycle("Inspected ${modules.size} module(s): ${findings.size} finding(s).")
    }

    private fun inspectedModules(): List<InspectedModule> {
        val jarHits = artifacts.get().associate { jar -> jar.coordinate to HitsJson.parse(jar.report.readText()) }
        val pomArtifacts = poms.get().artifacts
        val pomHits = pomArtifacts.associate { pom -> pom.coordinate to PomScanner.scan(pom.report.readBytes()) }
        val coordinates = jarHits.keys + pomHits.keys
        return coordinates.map { coordinate ->
            InspectedModule(coordinate, jarHits[coordinate].orEmpty() + pomHits[coordinate].orEmpty())
        }
    }

    private fun statusOf(problems: List<String>): DocumentStatus {
        if (problems.isEmpty()) return DocumentStatus.COMPLETE
        return DocumentStatus.INCOMPLETE
    }
}
