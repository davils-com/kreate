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

import com.davils.kreate.module.project.tests.TestsExtension
import com.davils.kreate.module.project.tests.logging.TestsLoggingExtension
import com.davils.kreate.module.project.tests.report.TestsReportExtension
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * One named body of tests, with its own source set, configurations and test task.
 *
 * A suite is what the conventional `test` source set should have been: a name, a place on
 * disk, a dependency scope and a task, all of which can exist more than once in a project.
 * The two Kreate registers by default are `unitTest` and `integrationTest`, and the split
 * matters because the two have opposite requirements. Unit tests are fast, hermetic and
 * belong on `check`; integration tests start containers, talk to real services, and have no
 * business running on every build. Keeping them in one source set forces the slower of the
 * two answers onto both.
 *
 * Every execution setting that also exists on [TestsExtension] inherits its value from there,
 * so a project sets a timeout once and overrides it only where a suite genuinely differs.
 *
 * On Kotlin/JVM a suite called `integrationTest` gives `src/integrationTest/kotlin`, the
 * `integrationTestImplementation` configuration and an `integrationTest` task. On Kotlin
 * Multiplatform it gives `src/commonIntegrationTest/kotlin` plus `src/jvmIntegrationTest/kotlin`
 * for every JVM target, a `jvmIntegrationTest` task per target, and an `integrationTest`
 * lifecycle task over them.
 *
 * @since 3.0.0
 */
public abstract class TestSuiteExtension @Inject constructor(
    /**
     * The name of this suite, used for its task and, by default, its source set.
     * @since 3.0.0
     */
    public val name: String,
    defaults: TestsExtension,
    factory: ObjectFactory
) {
    /**
     * Whether this suite is created at all.
     *
     * Defaults to `true`. Setting it to `false` on a pre-registered suite is how a project
     * opts out of `integrationTest` without having to unregister it.
     *
     * @since 3.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * The name of the source set backing this suite.
     *
     * Defaults to the suite name. On multiplatform projects this is the name of the shared
     * source set tree, from which `common<Name>` and `<target><Name>` are derived.
     *
     * @since 3.0.0
     */
    public val sourceSetName: Property<String> =
        factory.property(String::class.java).convention(name)

    /**
     * The source directories of this suite, replacing the convention when set.
     *
     * Empty by default, which leaves the Kotlin plugin's own layout in place. Paths are
     * resolved relative to the project directory.
     *
     * @since 3.0.0
     */
    public val srcDirs: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())

    /**
     * The description shown for this suite's task.
     *
     * @since 3.0.0
     */
    public val description: Property<String> = factory.property(String::class.java)
        .convention("Runs the $name suite.")

    /**
     * Whether this suite's compilation is associated with `main`.
     *
     * Defaults to `true`, which is what makes `internal` declarations visible to the tests and
     * what puts `main`'s dependencies on the suite's classpath. Without it a suite can only
     * exercise the published surface, which is a deliberate choice for a black box suite and
     * a mistake everywhere else.
     *
     * @since 3.0.0
     */
    public val associateWithMain: Property<Boolean> =
        factory.property(Boolean::class.java).convention(true)

    /**
     * The names of other suites whose compiled output this suite can use.
     *
     * The mechanism for shared fixtures: an `integrationTest` suite that lists `unitTest` sees
     * its helper classes and its `internal` declarations without either suite having to
     * publish them.
     *
     * @since 3.0.0
     */
    public val dependsOnSuites: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())

    /**
     * Whether `check` runs this suite.
     *
     * Defaults to `true`, and to `false` on the pre-registered `integrationTest` suite. A
     * suite that needs Docker or a network cannot be a precondition of every local build.
     *
     * @since 3.0.0
     */
    public val runOnCheck: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * The names of suites that must run before this one when both are in the same build.
     *
     * Ordering only - naming a suite here does not pull it into the build. `integrationTest`
     * lists `unitTest` by default, so a build that runs both reports the cheap failures first.
     *
     * @since 3.0.0
     */
    public val mustRunAfterSuites: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())

    /**
     * The names of the Kotlin targets this suite is created for, on multiplatform projects.
     *
     * Empty by default, which means every JVM target. Only JVM targets are supported: the
     * Kotlin plugin ties the test binaries of Native, JS and Wasm targets to their `test`
     * compilation with no way to point them elsewhere, so naming one here fails the build
     * rather than producing a source set nothing ever runs.
     *
     * Ignored on Kotlin/JVM projects.
     *
     * @since 3.0.0
     */
    public val targets: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())

    /**
     * The maximum number of parallel forks for this suite.
     *
     * Inherited from the enclosing `tests { }` block. Worth overriding to `1` on a suite
     * whose tests share an external resource such as a database container.
     *
     * @since 3.0.0
     */
    public val maxParallelForks: Property<Int> =
        factory.property(Int::class.java).convention(defaults.maxParallelForks)

    /**
     * The timeout for this suite, in minutes.
     *
     * Inherited from the enclosing `tests { }` block.
     *
     * @since 3.0.0
     */
    public val timeoutMinutes: Property<Long> =
        factory.property(Long::class.java).convention(defaults.timeoutMinutes)

    /**
     * Whether failures in this suite are ignored.
     *
     * Inherited from the enclosing `tests { }` block.
     *
     * @since 3.0.0
     */
    public val ignoreFailures: Property<Boolean> =
        factory.property(Boolean::class.java).convention(defaults.ignoreFailures)

    /**
     * Whether this suite runs even when its inputs are unchanged.
     *
     * Inherited from the enclosing `tests { }` block.
     *
     * @since 3.0.0
     */
    public val alwaysRun: Property<Boolean> =
        factory.property(Boolean::class.java).convention(defaults.alwaysRunTests)

    /**
     * Whether an empty suite fails the build.
     *
     * Inherited from the enclosing `tests { }` block.
     *
     * @since 3.0.0
     */
    public val failOnNoDiscoveredTests: Property<Boolean> =
        factory.property(Boolean::class.java).convention(defaults.failOnNoDiscoveredTests)

    /**
     * The JUnit Platform tags this suite runs, to the exclusion of everything else.
     *
     * Empty by default, which runs every test in the source set.
     *
     * @since 3.0.0
     */
    public val includeTags: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())

    /**
     * The JUnit Platform tags this suite skips.
     *
     * @since 3.0.0
     */
    public val excludeTags: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())

    /**
     * System properties passed to this suite's test JVM.
     *
     * @since 3.0.0
     */
    public val systemProperties: MapProperty<String, String> =
        factory.mapProperty(String::class.java, String::class.java).convention(emptyMap())

    /**
     * Environment variables set for this suite's test JVM.
     *
     * The place for the switches an integration suite needs, such as a Testcontainers
     * reuse or Ryuk setting, without putting them on every other test task in the project.
     *
     * @since 3.0.0
     */
    public val environment: MapProperty<String, String> =
        factory.mapProperty(String::class.java, String::class.java).convention(emptyMap())

    /**
     * Additional JVM arguments for this suite's test JVM.
     *
     * @since 3.0.0
     */
    public val jvmArgs: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())

    /**
     * Console logging for this suite.
     *
     * Every value is inherited from the enclosing `tests { }` block unless set here.
     *
     * @since 3.0.0
     */
    public val logging: TestsLoggingExtension =
        factory.newInstance(TestsLoggingExtension::class.java).inheritFrom(defaults.logging)

    /**
     * Report output for this suite.
     *
     * Every value is inherited from the enclosing `tests { }` block unless set here.
     *
     * @since 3.0.0
     */
    public val report: TestsReportExtension =
        factory.newInstance(TestsReportExtension::class.java).inheritFrom(defaults.report)

    /**
     * The dependencies of this suite.
     *
     * Declared here rather than in the project's top level `dependencies { }` block because the
     * suite's configurations do not exist yet while the build script is running.
     *
     * @since 3.0.0
     */
    internal val declaredDependencies: TestSuiteDependencies =
        factory.newInstance(TestSuiteDependencies::class.java)

    /**
     * The Kotest bundle for this suite.
     *
     * Every value is inherited from the enclosing `tests { }` block unless set here.
     *
     * @since 3.0.0
     */
    public val kotest: TestSuiteKotestExtension =
        factory.newInstance(TestSuiteKotestExtension::class.java).inheritFrom(defaults.kotest)

    /**
     * Configures the [TestsLoggingExtension] for this suite.
     *
     * @param action The configuration action.
     * @since 3.0.0
     */
    public fun logging(action: Action<TestsLoggingExtension>) {
        action.execute(logging)
    }

    /**
     * Configures the [TestsReportExtension] for this suite.
     *
     * @param action The configuration action.
     * @since 3.0.0
     */
    public fun report(action: Action<TestsReportExtension>) {
        action.execute(report)
    }

    /**
     * Configures the [TestSuiteDependencies] of this suite.
     *
     * @param action The configuration action.
     * @since 3.0.0
     */
    public fun dependencies(action: Action<TestSuiteDependencies>) {
        action.execute(declaredDependencies)
    }

    /**
     * Configures the [TestSuiteKotestExtension] for this suite.
     *
     * @param action The configuration action.
     * @since 3.0.0
     */
    public fun kotest(action: Action<TestSuiteKotestExtension>) {
        action.execute(kotest)
    }
}
