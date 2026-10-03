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

package com.davils.kreate.testing.suite

import com.davils.kreate.testing.TestSuiteKotestExtension
import com.davils.kreate.testing.TestsLoggingExtension
import com.davils.kreate.testing.TestsReportExtension

internal fun TestsLoggingExtension.inheritFrom(defaults: TestsLoggingExtension): TestsLoggingExtension {
    logPassedTests.convention(defaults.logPassedTests)
    logSkippedTests.convention(defaults.logSkippedTests)
    logTestStarted.convention(defaults.logTestStarted)
    return this
}

internal fun TestsReportExtension.inheritFrom(defaults: TestsReportExtension): TestsReportExtension {
    enabled.convention(defaults.enabled)
    xml.convention(defaults.xml)
    html.convention(defaults.html)
    return this
}

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
