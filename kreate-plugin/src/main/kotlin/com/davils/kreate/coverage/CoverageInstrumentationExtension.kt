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

package com.davils.kreate.coverage

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Controls which classes are instrumented and which test tasks run instrumented at all.
 *
 * Instrumentation is what makes coverage measurable, and it is not free: every instrumented class
 * carries counter bookkeeping at runtime. That is irrelevant for a unit test and very relevant
 * for a benchmark or a load test, where the instrumented timings are not the timings being
 * measured. [disabledForTestTasks] exists for exactly that case.
 *
 * Excluding a class here differs from excluding it in a report filter: an uninstrumented class
 * produces no data at all, whereas a filtered class produces data that is then left out of one
 * particular report.
 *
 * @param factory The object factory used for creating properties.
 * @since 2.2.0
 */
public abstract class CoverageInstrumentationExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 2.2.0
     */
    factory: ObjectFactory
) {
    /**
     * Whether instrumentation is switched off for every test task.
     *
     * Defaults to `false`. Setting this to `true` leaves the reporting tasks in place but leaves
     * them with nothing to report, which is occasionally useful as a temporary escape hatch.
     *
     * @since 2.2.0
     */
    public val disabledForAll: Property<Boolean> =
        factory.property(Boolean::class.java).convention(false)

    /**
     * The names of the test tasks that run without instrumentation.
     *
     * @since 2.2.0
     */
    public val disabledForTestTasks: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())

    /**
     * Fully qualified names of classes that are not instrumented, wildcards allowed.
     *
     * @since 2.2.0
     */
    public val excludedClasses: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())

    /**
     * Fully qualified names of the only classes that are instrumented, wildcards allowed.
     *
     * Empty by default, which instruments everything not named in [excludedClasses].
     *
     * @since 2.2.0
     */
    public val includedClasses: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())
}
