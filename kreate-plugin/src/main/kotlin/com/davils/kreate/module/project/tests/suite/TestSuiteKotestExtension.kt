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

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * The one dependency bundle Kreate is willing to declare for a test suite.
 *
 * Everything else a suite needs - Testcontainers, drivers, fixtures, an HTTP client - is the
 * project's own business and is declared against the suite's configurations
 * (`integrationTestImplementation` and friends). Kotest is the exception because it is the
 * framework the suite runs on rather than something the suite talks to: without it there is
 * no test engine, and pinning its version across a repository is the kind of decision a build
 * convention exists to make.
 *
 * Disabled by default, so a project that brings its own framework is unaffected.
 *
 * @param factory The object factory used for creating properties.
 * @since 3.0.0
 */
public abstract class TestSuiteKotestExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 3.0.0
     */
    factory: ObjectFactory
) {
    /**
     * Whether Kreate adds the Kotest artifacts to this suite.
     *
     * Defaults to `false`.
     *
     * @since 3.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The Kotest version to use for every artifact in the bundle.
     *
     * One version for the whole bundle: the Kotest modules are released together and mixing
     * them produces link errors rather than a resolution failure.
     *
     * @since 3.0.0
     */
    public val version: Property<String> =
        factory.property(String::class.java).convention(DEFAULT_KOTEST_VERSION)

    /**
     * The optional Kotest libraries added alongside the runner.
     *
     * Defaults to [KotestModule.ASSERTIONS]. The runner itself is not listed: it follows from
     * the platform and is always added when the bundle is enabled.
     *
     * @since 3.0.0
     */
    public val modules: ListProperty<KotestModule> = factory.listProperty(KotestModule::class.java)
        .convention(listOf(KotestModule.ASSERTIONS))

    /**
     * Whether the JUnit Platform launcher is added to the suite's runtime classpath.
     *
     * Defaults to `true`, and it has to. Gradle adds a launcher on its own only to the `test`
     * suite it created itself; a suite's task is registered by Kreate, so without this it fails
     * before it runs a single test with "Failed to load JUnit Platform". The switch exists for
     * a project that would rather declare the launcher itself, not as a default worth changing.
     *
     * Applies to every suite, whether or not the Kotest bundle is enabled - the launcher is
     * what starts the JUnit Platform, not part of any one framework.
     *
     * @since 3.0.0
     */
    public val addJUnitPlatformLauncher: Property<Boolean> =
        factory.property(Boolean::class.java).convention(true)

    /**
     * The version of the JUnit Platform launcher added by [addJUnitPlatformLauncher].
     *
     * Defaults to the JUnit 6 line. A project still on JUnit 5 should set the matching
     * `1.x` version here, because the launcher and the engines it starts are released and
     * versioned together.
     *
     * @since 3.0.0
     */
    public val junitPlatformVersion: Property<String> =
        factory.property(String::class.java).convention(DEFAULT_JUNIT_PLATFORM_VERSION)

    private companion object {
        /**
         * The Kotest version used when none is configured.
         */
        const val DEFAULT_KOTEST_VERSION = "6.2.4"

        /**
         * The JUnit Platform version used when none is configured.
         */
        const val DEFAULT_JUNIT_PLATFORM_VERSION = "6.1.3"
    }
}
