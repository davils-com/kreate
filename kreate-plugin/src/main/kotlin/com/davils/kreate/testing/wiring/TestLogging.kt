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

package com.davils.kreate.testing.wiring

import com.davils.kreate.testing.TestsLoggingExtension
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

internal fun AbstractTestTask.configureLogging(loggingExtension: TestsLoggingExtension) {
    testLogging {
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
        events = loggedEvents(loggingExtension)
    }
}

private fun loggedEvents(loggingExtension: TestsLoggingExtension): Set<TestLogEvent> = buildSet {
    add(TestLogEvent.FAILED)
    if (loggingExtension.logPassedTests.get()) add(TestLogEvent.PASSED)
    if (loggingExtension.logSkippedTests.get()) add(TestLogEvent.SKIPPED)
    if (loggingExtension.logTestStarted.get()) add(TestLogEvent.STARTED)
}
