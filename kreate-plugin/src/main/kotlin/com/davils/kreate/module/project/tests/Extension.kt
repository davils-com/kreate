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

import com.davils.kreate.module.project.tests.logging.TestsLoggingExtension
import com.davils.kreate.module.project.tests.report.TestsReportExtension
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

public abstract class TestsExtension @Inject constructor(factory: ObjectFactory) {
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)
    public val maxParallelForks: Property<Int> = factory.property(Int::class.java).convention(Runtime.getRuntime().availableProcessors() / 2)
    public val timeoutMinutes: Property<Long> = factory.property(Long::class.java).convention(10L)
    public val ignoreFailures: Property<Boolean> = factory.property(Boolean::class.java).convention(false)
    public val alwaysRunTests: Property<Boolean> = factory.property(Boolean::class.java).convention(false)
    public val failOnNoDiscoveredTests: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    @get:Nested
    public abstract val logging: TestsLoggingExtension

    @get:Nested
    public abstract val report: TestsReportExtension

    public fun logging(action: Action<TestsLoggingExtension>) {
        action.execute(logging)
    }

    public fun report(action: Action<TestsReportExtension>) {
        action.execute(report)
    }
}