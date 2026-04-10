package com.davils.kreate.module.project.tests.logging

import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

internal fun Test.configureLogging(loggingExtension: TestsLoggingExtension) {
    testLogging {
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL

        val events = mutableSetOf(TestLogEvent.FAILED)
        if (loggingExtension.logPassedTests.get()) events.add(TestLogEvent.PASSED)
        if (loggingExtension.logSkippedTests.get()) events.add(TestLogEvent.SKIPPED)
        if (loggingExtension.logTestStarted.get()) events.add(TestLogEvent.STARTED)

        this.events = events
    }
}