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

import com.davils.kreate.coverage.CoverageBinaryReportSpec
import com.davils.kreate.coverage.CoverageFilterExtension
import com.davils.kreate.coverage.CoverageFilterSpec
import com.davils.kreate.coverage.CoverageHtmlReportSpec
import com.davils.kreate.coverage.CoverageLogReportSpec
import com.davils.kreate.coverage.CoverageReportExtension
import com.davils.kreate.coverage.CoverageXmlReportSpec
import kotlinx.kover.gradle.plugin.dsl.KoverBinaryTaskConfig
import kotlinx.kover.gradle.plugin.dsl.KoverHtmlTaskConfig
import kotlinx.kover.gradle.plugin.dsl.KoverLogTaskConfig
import kotlinx.kover.gradle.plugin.dsl.KoverReportFilter
import kotlinx.kover.gradle.plugin.dsl.KoverReportSetConfig
import kotlinx.kover.gradle.plugin.dsl.KoverXmlTaskConfig

internal fun KoverReportSetConfig.configureFilters(extension: CoverageFilterExtension) {
    filters {
        excludes { applyCriteria(extension.excludes) }
        includes { applyCriteria(extension.includes) }
    }
}

internal fun KoverReportSetConfig.configureReports(extension: CoverageReportExtension) {
    xml { applyXml(extension.xml) }
    html { applyHtml(extension.html) }
    log { applyLog(extension.log) }
    binary { applyBinary(extension.binary) }
}

private fun KoverReportFilter.applyCriteria(spec: CoverageFilterSpec) {
    classes.set(spec.classes)
    annotatedBy.set(spec.annotatedBy)
    inheritedFrom.set(spec.inheritedFrom)
    foldPackagesIntoClassPatterns(spec)
}

private fun KoverReportFilter.foldPackagesIntoClassPatterns(spec: CoverageFilterSpec) {
    packages(spec.packages.get())
}

private fun KoverXmlTaskConfig.applyXml(spec: CoverageXmlReportSpec) {
    onCheck.set(spec.onCheck)
    xmlFile.set(spec.file)
    if (spec.title.isPresent) title.set(spec.title)
}

private fun KoverHtmlTaskConfig.applyHtml(spec: CoverageHtmlReportSpec) {
    onCheck.set(spec.onCheck)
    htmlDir.set(spec.directory)
    if (spec.title.isPresent) title.set(spec.title)
    if (spec.charset.isPresent) charset.set(spec.charset)
}

private fun KoverLogTaskConfig.applyLog(spec: CoverageLogReportSpec) {
    onCheck.set(spec.onCheck)
    format.set(spec.format)
    groupBy.set(spec.groupBy.get().toKover())
    coverageUnits.set(spec.coverageUnit.get().toKover())
    aggregationForGroup.set(spec.aggregation.get().toKover())
    if (spec.header.isPresent) header.set(spec.header)
}

private fun KoverBinaryTaskConfig.applyBinary(spec: CoverageBinaryReportSpec) {
    onCheck.set(spec.onCheck)
    if (spec.file.isPresent) file.set(spec.file)
}
