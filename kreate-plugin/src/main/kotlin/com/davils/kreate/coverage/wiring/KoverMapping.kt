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

package com.davils.kreate.coverage.wiring

import com.davils.kreate.coverage.Aggregation
import com.davils.kreate.coverage.CoverageUnit
import com.davils.kreate.coverage.Grouping
import kotlinx.kover.gradle.plugin.dsl.AggregationType as KoverAggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit as KoverCoverageUnit
import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType as KoverGroupingEntityType

internal fun CoverageUnit.toKover(): KoverCoverageUnit = when (this) {
    CoverageUnit.LINE -> KoverCoverageUnit.LINE
    CoverageUnit.INSTRUCTION -> KoverCoverageUnit.INSTRUCTION
    CoverageUnit.BRANCH -> KoverCoverageUnit.BRANCH
}

internal fun Aggregation.toKover(): KoverAggregationType = when (this) {
    Aggregation.COVERED_COUNT -> KoverAggregationType.COVERED_COUNT
    Aggregation.MISSED_COUNT -> KoverAggregationType.MISSED_COUNT
    Aggregation.COVERED_PERCENTAGE -> KoverAggregationType.COVERED_PERCENTAGE
    Aggregation.MISSED_PERCENTAGE -> KoverAggregationType.MISSED_PERCENTAGE
}

internal fun Grouping.toKover(): KoverGroupingEntityType = when (this) {
    Grouping.APPLICATION -> KoverGroupingEntityType.APPLICATION
    Grouping.CLASS -> KoverGroupingEntityType.CLASS
    Grouping.PACKAGE -> KoverGroupingEntityType.PACKAGE
}
