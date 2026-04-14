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

package com.davils.kreate.module.project.tests

import com.davils.kreate.KreateExtension
import com.davils.kreate.module.project.tests.logging.configureLogging
import com.davils.kreate.module.project.tests.report.configureReport
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinTest
import java.time.Duration

internal fun Project.initializeTesting(extension: KreateExtension) {
    val testingExtension = extension.project.tests
    if (!testingExtension.enabled.get()) return

    validateKotestPlugin()

    fun Test.configureCommonJvmTesting() {
        timeout.set(Duration.ofMinutes(testingExtension.timeoutMinutes.get()))
        ignoreFailures = testingExtension.ignoreFailures.get()
        failOnNoDiscoveredTests.set(testingExtension.failOnNoDiscoveredTests.get())
        outputs.upToDateWhen { !testingExtension.alwaysRunTests.get() }
    }

    fun Test.configureJvmTestEngine() {
        useJUnitPlatform()
        maxParallelForks = testingExtension.maxParallelForks.get()
        configureLogging(testingExtension.logging)
        configureReport(testingExtension.report)
    }

    fun KotlinTest.configureCommonKmpTesting() {
        timeout.set(Duration.ofMinutes(testingExtension.timeoutMinutes.get()))
        ignoreFailures = testingExtension.ignoreFailures.get()
        failOnNoDiscoveredTests.set(testingExtension.failOnNoDiscoveredTests.get())
        outputs.upToDateWhen { !testingExtension.alwaysRunTests.get() }
        configureLogging(testingExtension.logging)
        configureReport(testingExtension.report)
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        tasks.withType<Test>().configureEach {
            configureCommonJvmTesting()
            configureJvmTestEngine()
        }
    }

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        tasks.withType<Test>().configureEach {
            configureCommonJvmTesting()
            configureJvmTestEngine()
        }

        tasks.withType<KotlinTest>().configureEach {
            configureCommonKmpTesting()
        }
    }
}