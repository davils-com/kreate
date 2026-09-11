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

import org.gradle.api.Project

/**
 * The Maven group every Kotest artifact lives in.
 *
 * @since 3.0.0
 */
private const val KOTEST_GROUP: String = "io.kotest"

/**
 * Which runner a suite's configuration needs.
 *
 * @since 3.0.0
 */
internal enum class SuitePlatform(
    /**
     * The artifact id of the runner for this platform.
     */
    val runnerArtifact: String
) {
    /**
     * A configuration resolved for the JVM, which runs Kotest through the JUnit Platform.
     */
    JVM("kotest-runner-junit5"),

    /**
     * A shared multiplatform configuration, which carries the platform-independent engine.
     */
    COMMON("kotest-framework-engine")
}

/**
 * Adds the suite's declared dependencies to its configurations.
 *
 * @param suite The suite configuration.
 * @param sourceSetName The name of the source set whose configurations to add to.
 * @since 3.0.0
 */
internal fun Project.addSuiteDependencies(suite: TestSuiteExtension, sourceSetName: String) {
    val declared = suite.declaredDependencies

    declared.platformDependencies.get().forEach { notation ->
        dependencies.add("${sourceSetName}Implementation", dependencies.platform(notation))
    }
    declared.implementationDependencies.get().forEach {
        dependencies.add("${sourceSetName}Implementation", it)
    }
    declared.compileOnlyDependencies.get().forEach {
        dependencies.add("${sourceSetName}CompileOnly", it)
    }
    declared.runtimeOnlyDependencies.get().forEach {
        dependencies.add("${sourceSetName}RuntimeOnly", it)
    }
}

/**
 * Adds the JUnit Platform launcher to a suite's runtime classpath.
 *
 * Gradle adds a launcher by itself only to the `test` suite it created; a suite's task is
 * registered by Kreate, and without the launcher it fails before running anything with a
 * message about the JUnit Platform that says nothing about where the launcher should have come
 * from. This is plumbing for the task, not a dependency the project chose.
 *
 * @param suite The suite configuration.
 * @param sourceSetName The name of the source set whose runtime classpath to add to.
 * @since 3.0.0
 */
internal fun Project.addJUnitPlatformLauncher(suite: TestSuiteExtension, sourceSetName: String) {
    if (!suite.kotest.addJUnitPlatformLauncher.get()) return
    dependencies.add(
        "${sourceSetName}RuntimeOnly",
        "org.junit.platform:junit-platform-launcher:${suite.kotest.junitPlatformVersion.get()}"
    )
}

/**
 * Adds the Kotest bundle to one of a suite's configurations.
 *
 * This is the one dependency Kreate declares on a project's behalf. Kotest is not something a
 * suite talks to, it is what the suite runs on, and a test source set without a framework is
 * an empty task that passes. Everything else a suite needs - Testcontainers, drivers, an HTTP
 * client - stays with the project, declared against the configurations created here.
 *
 * @param suite The suite configuration.
 * @param configurationName The dependency configuration to add to.
 * @param platform Which runner the configuration needs.
 * @since 3.0.0
 */
internal fun Project.addKotestBundle(
    suite: TestSuiteExtension,
    configurationName: String,
    platform: SuitePlatform
) {
    val kotest = suite.kotest
    if (!kotest.enabled.get()) return

    val version = kotest.version.get()
    dependencies.add(configurationName, "$KOTEST_GROUP:${platform.runnerArtifact}:$version")

    kotest.modules.get().forEach { module ->
        dependencies.add(configurationName, "$KOTEST_GROUP:${module.artifact}:$version")
    }
}
