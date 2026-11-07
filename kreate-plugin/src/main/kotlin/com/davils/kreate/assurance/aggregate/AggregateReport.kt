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
package com.davils.kreate.assurance.aggregate

import com.davils.kreate.assurance.policy.GateStatus
import java.time.Instant

private val STATUS_RANK: Map<GateStatus, Int> = mapOf(
    GateStatus.PASSED to 0,
    GateStatus.FINDINGS to 1,
    GateStatus.INCOMPLETE to 2,
    GateStatus.INVALID to 3
)

internal data class AggregateReport(
    val projects: List<ProjectSummary>,
    val unchecked: List<String>,
    val generatedAt: Instant
) {
    val status: GateStatus
        get() = projects.map { summary -> summary.status }.maxByOrNull { status -> STATUS_RANK.getValue(status) }
            ?: GateStatus.PASSED

    val blockingFindings: Int
        get() = projects.sumOf { summary -> summary.blockingFindings }
}
