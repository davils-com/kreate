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

package com.davils.kreate.module.project.tests.suite

import com.davils.kreate.module.project.tests.logging.TestsLoggingExtension
import com.davils.kreate.module.project.tests.report.TestsReportExtension

/**
 * Chains this block's conventions onto the matching block of the enclosing `tests { }`.
 *
 * The nested blocks of a suite cannot be Gradle managed properties: a managed instance is
 * constructed with no reference to the suite it belongs to, and there would be nothing to
 * inherit from. They are created by hand instead, which leaves their conventions pointing at
 * the hard-coded defaults, and this replaces those with the enclosing block's values.
 *
 * Re-setting a convention is safe: a convention only applies while no explicit value has been
 * set, and none has been at the point this runs.
 *
 * @param defaults The logging block of the enclosing `tests { }`.
 * @return This block.
 * @since 3.0.0
 */
internal fun TestsLoggingExtension.inheritFrom(defaults: TestsLoggingExtension): TestsLoggingExtension {
    logPassedTests.convention(defaults.logPassedTests)
    logSkippedTests.convention(defaults.logSkippedTests)
    logTestStarted.convention(defaults.logTestStarted)
    return this
}

/**
 * Chains this block's conventions onto the matching block of the enclosing `tests { }`.
 *
 * @param defaults The report block of the enclosing `tests { }`.
 * @return This block.
 * @since 3.0.0
 */
internal fun TestsReportExtension.inheritFrom(defaults: TestsReportExtension): TestsReportExtension {
    enabled.convention(defaults.enabled)
    xml.convention(defaults.xml)
    html.convention(defaults.html)
    return this
}

/**
 * Chains this block's conventions onto the matching block of the enclosing `tests { }`.
 *
 * @param defaults The Kotest block of the enclosing `tests { }`.
 * @return This block.
 * @since 3.0.0
 */
internal fun TestSuiteKotestExtension.inheritFrom(
    defaults: TestSuiteKotestExtension
): TestSuiteKotestExtension {
    enabled.convention(defaults.enabled)
    version.convention(defaults.version)
    modules.convention(defaults.modules)
    addJUnitPlatformLauncher.convention(defaults.addJUnitPlatformLauncher)
    junitPlatformVersion.convention(defaults.junitPlatformVersion)
    return this
}
