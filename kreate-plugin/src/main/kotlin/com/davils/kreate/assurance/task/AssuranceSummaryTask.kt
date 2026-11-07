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

import com.davils.kreate.assurance.finding.AssuranceInputException
import com.davils.kreate.assurance.report.JsonReportWriter
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "The summary depends on the day it is made")
internal abstract class AssuranceSummaryTask :
    AssuranceEvaluationTask("Summarises the gate of this project for the aggregation.") {
    @get:OutputFile
    public abstract val summaryFile: RegularFileProperty

    @TaskAction
    public fun summarise() {
        val evaluation = try {
            evaluate()
        } catch (cause: AssuranceInputException) {
            throw AssuranceInvalidFailure(cause.message.orEmpty(), cause)
        }
        val file = summaryFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(JsonReportWriter.render(evaluation))
    }
}
