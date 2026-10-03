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

import com.davils.kreate.testing.TestSuiteExtension
import org.gradle.api.tasks.testing.Test
import java.time.Duration

internal fun Test.configureSuite(suite: TestSuiteExtension) {
    val alwaysRun = suite.alwaysRun.get()

    description = suite.description.get()
    timeout.set(Duration.ofMinutes(suite.timeoutMinutes.get()))
    ignoreFailures = suite.ignoreFailures.get()
    failOnNoDiscoveredTests.set(suite.failOnNoDiscoveredTests.get())
    outputs.upToDateWhen { !alwaysRun }
    maxParallelForks = suite.maxParallelForks.get()

    useJUnitPlatformWithTagFilters(suite.includeTags.get(), suite.excludeTags.get())

    suite.systemProperties.get().forEach { (key, value) -> systemProperty(key, value) }
    suite.environment.get().forEach { (key, value) -> environment(key, value) }
    jvmArgs(suite.jvmArgs.get())

    configureLogging(suite.logging)
    configureReport(suite.report)
}

private fun Test.useJUnitPlatformWithTagFilters(includedTags: List<String>, excludedTags: List<String>) {
    useJUnitPlatform {
        if (includedTags.isNotEmpty()) includeTags(*includedTags.toTypedArray())
        if (excludedTags.isNotEmpty()) excludeTags(*excludedTags.toTypedArray())
    }
}
