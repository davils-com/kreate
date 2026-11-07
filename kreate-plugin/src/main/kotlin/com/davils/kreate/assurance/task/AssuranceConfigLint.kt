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
package com.davils.kreate.assurance.task

import com.davils.kreate.assurance.config.ConfigLintInput
import com.davils.kreate.assurance.config.ConfigLinter
import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.time.Instant
import java.time.LocalDate

@DisableCachingByDefault(because = "Ignore rules expire as the days pass")
internal abstract class AssuranceConfigLint : FindingCollectTask(
    "Checks the assurance configuration for settings that weaken the gate.",
    KreateTaskGroup.ASSURANCE
) {
    @get:Input
    public abstract val values: MapProperty<String, String>

    @get:Input
    public abstract val urls: MapProperty<String, String>

    @get:Input
    public abstract val ignoreRules: ListProperty<IgnoreRuleInput>

    @get:Input
    public abstract val today: Property<String>

    @get:Internal
    public abstract val baselineFile: ConfigurableFileCollection

    @TaskAction
    public fun lint() {
        val input = ConfigLintInput(
            values = values.get(),
            ignoreRules = ignoreRules.get(),
            urls = urls.get(),
            baselineExists = baselineFile.any { file -> file.isFile },
            today = LocalDate.parse(today.get())
        )
        val findings = ConfigLinter.lint(input)
        writeDocument(FindingDocument(FindingSource.CONFIG, DocumentStatus.COMPLETE, Instant.now(), findings))
        logger.lifecycle("Checked the assurance configuration: ${findings.size} finding(s).")
        findings.forEach { finding -> logger.lifecycle("${finding.severity.name} ${finding.ruleId}: ${finding.title}") }
    }
}
