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

import com.davils.kreate.assurance.Severity
import com.davils.kreate.policy.check.Strictness
import com.davils.kreate.policy.model.AssurancePolicy
import com.davils.kreate.policy.model.AssurancePolicyWriter
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

private const val SWITCH_SUFFIX: String = "enabled"

@DisableCachingByDefault(because = "It only writes a template")
internal abstract class AssurancePolicyTemplate : KreateTask(
    "Writes a policy that requires the current assurance settings, as a start for an organisation policy.",
    KreateTaskGroup.ASSURANCE
) {
    @get:Input
    public abstract val policyName: Property<String>

    @get:Input
    public abstract val values: MapProperty<String, String>

    @get:Input
    public abstract val forbidIgnoreOf: SetProperty<Severity>

    @get:OutputFile
    public abstract val outputFile: RegularFileProperty

    @TaskAction
    public fun write() {
        val current = values.get()
        val switches = current.filter { (key, value) -> key.endsWith(SWITCH_SUFFIX) && value == "true" }
        val minimums = current.filter { (key, value) -> key in Strictness.OF_OPTION && value.isNotEmpty() }
        val policy = AssurancePolicy(
            name = policyName.get(),
            require = switches + ("reportOnly" to "false"),
            minimum = minimums,
            forbidIgnoreOf = forbidIgnoreOf.get(),
            ignoreRulesNeedExpiry = true
        )
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(AssurancePolicyWriter.render(policy))
        logger.lifecycle("Wrote ${file.path}. Publish it and let CI pass -Pkreate.assurance.policy=g:a:v.")
    }
}
