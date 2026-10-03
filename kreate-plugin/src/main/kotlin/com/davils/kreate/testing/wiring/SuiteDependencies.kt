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
import com.davils.kreate.testing.suite.SuitePlatform
import com.davils.kreate.testing.suite.compileOnlyName
import com.davils.kreate.testing.suite.implementationName
import com.davils.kreate.testing.suite.runtimeOnlyName
import org.gradle.api.Project

private const val KOTEST_GROUP: String = "io.kotest"

private const val JUNIT_PLATFORM_LAUNCHER: String = "org.junit.platform:junit-platform-launcher"

internal fun Project.addSuiteDependencies(suite: TestSuiteExtension, sourceSetName: String) {
    val declared = suite.declaredDependencies
    val implementation = implementationName(sourceSetName)

    declared.platformDependencies.get().forEach { notation ->
        dependencies.add(implementation, dependencies.platform(notation))
    }
    declared.implementationDependencies.get().forEach { notation ->
        dependencies.add(implementation, notation)
    }
    declared.compileOnlyDependencies.get().forEach { notation ->
        dependencies.add(compileOnlyName(sourceSetName), notation)
    }
    declared.runtimeOnlyDependencies.get().forEach { notation ->
        dependencies.add(runtimeOnlyName(sourceSetName), notation)
    }
}

internal fun Project.addJUnitPlatformLauncher(suite: TestSuiteExtension, sourceSetName: String) {
    val kotest = suite.kotest
    if (!kotest.addJUnitPlatformLauncher.get()) return

    val launcher = "$JUNIT_PLATFORM_LAUNCHER:${kotest.junitPlatformVersion.get()}"
    dependencies.add(runtimeOnlyName(sourceSetName), launcher)
}

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
