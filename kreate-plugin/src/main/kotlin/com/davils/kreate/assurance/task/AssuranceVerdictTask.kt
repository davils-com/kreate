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
import com.davils.kreate.assurance.policy.GateStatus
import com.davils.kreate.assurance.policy.Verdict
import com.davils.kreate.assurance.report.JsonReportWriter
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "The verdict depends on the day it is made")
internal abstract class AssuranceVerdictTask : AssuranceEvaluationTask("Decides whether the build passes the gate.") {
    @get:OutputFile
    public abstract val resultFile: RegularFileProperty

    @TaskAction
    public fun decide() {
        val verdict = try {
            evaluate().verdict
        } catch (cause: AssuranceInputException) {
            writeResult(Verdict(GateStatus.INVALID, 0, emptyList(), 0))
            throw AssuranceInvalidFailure(cause.message.orEmpty(), cause)
        }

        writeResult(verdict)
        fail(verdict)
    }

    private fun writeResult(verdict: Verdict) {
        val file = resultFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(JsonReportWriter.renderResult(verdict))
    }

    private fun fail(verdict: Verdict) {
        when (verdict.status) {
            GateStatus.PASSED -> logger.lifecycle("Kreate assurance [PASSED]: no finding blocks this build.")
            GateStatus.FINDINGS -> throw AssuranceFindingsFailure(findingsMessage(verdict))
            GateStatus.INCOMPLETE -> throw AssuranceIncompleteFailure(incompleteMessage(verdict))
            GateStatus.INVALID -> error("An invalid input fails while reading it.")
        }
    }

    private fun findingsMessage(verdict: Verdict): String =
        "${verdict.blockingFindings} blocking finding(s), ${verdict.staleEntries} stale baseline entries. " +
            "See the report of $path."

    private fun incompleteMessage(verdict: Verdict): String {
        val sources = verdict.incompleteSources.joinToString()
        return "These sources could not finish, so nothing can be said about them: $sources."
    }
}
