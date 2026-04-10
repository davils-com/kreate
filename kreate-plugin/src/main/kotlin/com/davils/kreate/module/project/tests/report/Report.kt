package com.davils.kreate.module.project.tests.report

import org.gradle.api.tasks.testing.AbstractTestTask

internal fun AbstractTestTask.configureReport(reportExtension: TestsReportExtension) {
    if (!reportExtension.enabled.get()) {
        return
    }

    if (reportExtension.xml.get()) {
        reports.junitXml.required.set(true)
    }

    if (reportExtension.html.get()) {
        reports.html.required.set(true)
    }
}