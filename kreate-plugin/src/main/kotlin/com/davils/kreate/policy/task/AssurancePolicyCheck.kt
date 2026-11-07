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
package com.davils.kreate.policy.task

import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.task.FindingCollectTask
import com.davils.kreate.assurance.task.IgnoreRuleInput
import com.davils.kreate.dependency.ResolvedGraph
import com.davils.kreate.policy.check.PolicyChecker
import com.davils.kreate.policy.check.PolicyInput
import com.davils.kreate.policy.model.AssurancePolicyJson
import com.davils.kreate.policy.model.PolicyFormatException
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.time.Instant
import java.time.LocalDate

@DisableCachingByDefault(because = "Ignore rules expire as the days pass")
internal abstract class AssurancePolicyCheck : FindingCollectTask(
    "Checks the build against the policy named by kreate.assurance.policy.",
    KreateTaskGroup.ASSURANCE
) {
    @get:Input
    public abstract val notation: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val policyFiles: ConfigurableFileCollection

    @get:Input
    public abstract val failures: ListProperty<String>

    @get:Input
    public abstract val values: MapProperty<String, String>

    @get:Input
    public abstract val ignoreRules: ListProperty<IgnoreRuleInput>

    @get:Input
    public abstract val graphs: ListProperty<ResolvedGraph>

    @get:Input
    public abstract val today: Property<String>

    @TaskAction
    public fun check() {
        val source = notation.get()
        val file = policyFiles.files.firstOrNull { candidate -> candidate.isFile }
        if (file == null) return incomplete("The policy $source could not be read: ${failureText()}")
        val policy = try {
            AssurancePolicyJson.parse(file.readText())
        } catch (cause: PolicyFormatException) {
            throw InvalidUserDataException("The policy $source is invalid: ${cause.message}", cause)
        }
        val listed = graphs.get().flatMap { graph -> graph.modules }
        val modules = listed.distinct()
        val input = PolicyInput(values.get(), ignoreRules.get(), modules, LocalDate.parse(today.get()))
        val findings = try {
            PolicyChecker(policy).check(input)
        } catch (cause: PolicyFormatException) {
            throw InvalidUserDataException(cause.message.orEmpty(), cause)
        }
        writeDocument(FindingDocument(FindingSource.POLICY, DocumentStatus.COMPLETE, Instant.now(), findings))
        logger.lifecycle("Checked the build against ${policy.name} ($source): ${findings.size} violation(s).")
        findings.forEach { finding -> logger.lifecycle("${finding.ruleId}: ${finding.title}") }
    }

    private fun failureText(): String {
        val text = failures.get().joinToString()
        return text.ifEmpty { "it does not exist" }
    }

    private fun incomplete(reason: String) {
        writeDocument(FindingDocument(FindingSource.POLICY, DocumentStatus.INCOMPLETE, Instant.now(), reason = reason))
        logger.warn(reason)
    }
}
