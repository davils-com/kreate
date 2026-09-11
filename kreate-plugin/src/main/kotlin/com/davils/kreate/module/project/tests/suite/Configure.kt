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

import com.davils.kreate.module.project.tests.logging.configureLogging
import com.davils.kreate.module.project.tests.report.configureReport
import org.gradle.api.tasks.testing.Test
import java.time.Duration

/**
 * Applies a suite's configuration to its test task.
 *
 * Every value is read into a local before it is used. That is not a style choice: an
 * `upToDateWhen` spec is serialised into the configuration cache entry, so a lambda that
 * reaches back into the extension drags the whole project model in with it and fails the
 * build, which this project has configured to happen rather than be warned about.
 *
 * @param suite The suite this task runs.
 * @since 3.0.0
 */
internal fun Test.configureSuite(suite: TestSuiteExtension) {
    val alwaysRun = suite.alwaysRun.get()
    val includedTags = suite.includeTags.get()
    val excludedTags = suite.excludeTags.get()

    description = suite.description.get()
    timeout.set(Duration.ofMinutes(suite.timeoutMinutes.get()))
    ignoreFailures = suite.ignoreFailures.get()
    failOnNoDiscoveredTests.set(suite.failOnNoDiscoveredTests.get())
    outputs.upToDateWhen { !alwaysRun }
    maxParallelForks = suite.maxParallelForks.get()

    // Re-declared rather than left to the project-wide configuration: that one calls
    // `useJUnitPlatform()` with no arguments, and whichever of the two runs last wins, so the
    // tag filters have to be set by the same call that selects the platform.
    useJUnitPlatform {
        if (includedTags.isNotEmpty()) includeTags(*includedTags.toTypedArray())
        if (excludedTags.isNotEmpty()) excludeTags(*excludedTags.toTypedArray())
    }

    suite.systemProperties.get().forEach { (key, value) -> systemProperty(key, value) }
    suite.environment.get().forEach { (key, value) -> environment(key, value) }
    jvmArgs(suite.jvmArgs.get())

    configureLogging(suite.logging)
    configureReport(suite.report)
}
