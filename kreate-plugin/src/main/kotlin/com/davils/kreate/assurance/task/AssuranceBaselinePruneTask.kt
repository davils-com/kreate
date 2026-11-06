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

import com.davils.kreate.assurance.baseline.Baseline
import com.davils.kreate.assurance.baseline.BaselineJson
import com.davils.kreate.assurance.finding.AssuranceInputException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "It rewrites a file that is under version control")
internal abstract class AssuranceBaselinePruneTask :
    AssuranceEvaluationTask("Removes what no longer occurs from the baseline.") {
    @get:Internal
    public abstract val target: RegularFileProperty

    @TaskAction
    public fun prune() {
        val pruned = try {
            val documents = readDocuments()
            Baseline.prune(readBaseline(), findingsOf(documents), completeSourcesOf(documents))
        } catch (cause: AssuranceInputException) {
            throw AssuranceInvalidFailure(cause.message.orEmpty(), cause)
        }

        val file = target.get().asFile
        file.writeText(BaselineJson.render(pruned))
        logger.lifecycle("The baseline keeps ${pruned.size} entries.")
    }
}
