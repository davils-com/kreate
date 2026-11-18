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
import com.davils.kreate.testing.suite.implementationName
import com.davils.kreate.testing.suite.sharedSourceSetName
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.ExecutionTaskHolder
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTests
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

internal fun Project.shareSuiteWithNonJvmTargets(
    suites: List<TestSuiteExtension>,
    kotlin: KotlinMultiplatformExtension,
    tasksBySuite: Map<String, TaskProvider<Task>>
) {
    val sharing = suites.filter { suite -> suite.nonJvmTargets.get() }
    if (sharing.size > 1) throw tooManySharingSuites(sharing)
    val suite = sharing.singleOrNull() ?: return

    val sharedName = sharedSourceSetName(suite.sourceSetName.get())
    val shared = kotlin.sourceSets.getByName(sharedName)
    val targets = kotlin.targets.filterIsInstance<KotlinTargetWithTests<*, *>>().filterNot { it.isJvm() }
    targets.forEach { target -> shareWith(target, suite, shared) }

    val testRuns = targets.flatMap { target -> target.testRuns.filterIsInstance<ExecutionTaskHolder<*>>() }
    val testTasks = testRuns.map { run -> run.executionTask }
    tasksBySuite[suite.name]?.configure { dependsOn(testTasks) }
}

private fun KotlinTarget.isJvm(): Boolean = this is KotlinJvmTarget

private fun Project.shareWith(target: KotlinTarget, suite: TestSuiteExtension, shared: KotlinSourceSet) {
    val test = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)
    val testSourceSet = test.defaultSourceSet
    testSourceSet.kotlin.srcDirs(shared.kotlin.srcDirs)
    testSourceSet.resources.srcDirs(shared.resources.srcDirs)

    addSuiteDependencies(suite, testSourceSet.name)
    addKotestBundle(suite, implementationName(testSourceSet.name), SuitePlatform.NON_JVM)
}

private fun Project.tooManySharingSuites(sharing: List<TestSuiteExtension>): GradleException {
    val names = sharing.joinToString { suite -> suite.name }
    return GradleException(
        """
            The test suites $names of project '$path' all set nonJvmTargets = true.

            A JavaScript, Wasm or Native target runs its tests from a single compilation called
            'test', so only one suite can share its tests with those targets. Keep it on the suite
            whose tests should run there, usually unitTest.
        """.trimIndent()
    )
}
