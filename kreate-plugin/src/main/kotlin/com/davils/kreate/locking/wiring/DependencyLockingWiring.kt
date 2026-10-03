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

package com.davils.kreate.locking.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.local.LocalWorkflowTaskNames
import com.davils.kreate.locking.DependencyLockingTaskNames
import com.davils.kreate.settings.local.LocalMode
import com.davils.kreate.settings.local.gatherLocalModeInputs
import com.davils.kreate.settings.local.isActive
import com.davils.kreate.settings.local.resolveLocalMode
import com.davils.kreate.settings.local.workspace
import com.davils.kreate.task.KreateTaskGroup
import com.davils.kreate.testing.suite.enabledSuites
import com.davils.kreate.testing.suite.lowerCamelCaseName
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

private const val COMPILE_CLASSPATH_SUFFIX = "CompileClasspath"
private const val RUNTIME_CLASSPATH_SUFFIX = "RuntimeClasspath"
private const val SUBSTITUTION_SEPARATOR = "\n        "

internal fun Project.initializeDependencyLocking(extension: KreateExtension) {
    val lockingExtension = extension.project.dependencyLocking
    if (!lockingExtension.enabled.get()) return

    if (localModeOf(extension).isActive) {
        deactivateLockingForLocalMode()
        return
    }

    val lockedClasspaths = lockingExtension.lockedClasspaths.get() +
        multiplatformClasspaths() +
        testSuiteClasspaths(extension)

    if (lockingExtension.lockAllConfigurations.get()) {
        lockEveryConfiguration()
        return
    }

    lockClasspaths(lockedClasspaths)
}

private fun Project.lockEveryConfiguration() {
    dependencyLocking.lockAllConfigurations()
    registerResolveAndLockAll { true }
}

private fun Project.lockClasspaths(lockedClasspaths: Set<String>) {
    configurations.matching { it.name in lockedClasspaths }.configureEach {
        resolutionStrategy.activateDependencyLocking()
    }
    registerResolveAndLockAll { configurationName -> configurationName in lockedClasspaths }
}

private fun Project.localModeOf(extension: KreateExtension): LocalMode = resolveLocalMode(
    gatherLocalModeInputs(
        providers,
        gradle.gradleUserHomeDir,
        extension.local.ciEnvironmentVariables.get()
    )
)

private fun Project.deactivateLockingForLocalMode() {
    if (gradle.startParameter.isWriteDependencyLocks) {
        throw GradleException(localLockWriteRefusal(localSubstitutions()))
    }

    dependencyLocking.unlockAllConfigurations()
    logger.lifecycle(
        "Kreate local mode: dependency locking is off for '$path'. No lock file is read or written."
    )
}

private fun Project.localSubstitutions(): List<Map.Entry<String, String>> {
    val localMode = resolveLocalMode(
        gatherLocalModeInputs(providers, gradle.gradleUserHomeDir, emptyList())
    )
    val substitutions = localMode.workspace.substitutions
    return substitutions.entries.sortedBy { it.key }
}

private fun localLockWriteRefusal(substituted: List<Map.Entry<String, String>>): String =
    """
        Refusing to write lock files while resolving from the local Maven repository.

        These versions would have been recorded into a committed lock file:

        ${substituted.joinToString(SUBSTITUTION_SEPARATOR) { "${it.key}:${it.value}" }}

        They exist on this machine only, so the lock file would break every pipeline and
        every other checkout.

        Clear the local publications first:

            ./gradlew ${LocalWorkflowTaskNames.CLEAN}

        or write the locks with local mode off:

            ./gradlew ${DependencyLockingTaskNames.RESOLVE_AND_LOCK_ALL} --write-locks -Pkreate.local=false
    """.trimIndent()

private fun Project.multiplatformClasspaths(): Set<String> {
    val multiplatform = extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return emptySet()

    return multiplatform.targets.flatMapTo(mutableSetOf()) { target -> classpathsOf(target.name) }
}

private fun Project.testSuiteClasspaths(extension: KreateExtension): Set<String> {
    val tests = extension.project.tests
    if (!tests.enabled.get()) return emptySet()

    val multiplatform = extensions.findByType(KotlinMultiplatformExtension::class.java)

    return tests.enabledSuites().flatMapTo(mutableSetOf()) { suite ->
        val sourceSetName = suite.sourceSetName.get()
        val prefixes = multiplatform
            ?.targets
            ?.map { target -> lowerCamelCaseName(target.name, sourceSetName) }
            ?: listOf(sourceSetName)

        prefixes.flatMap { prefix -> classpathsOf(prefix) }
    }
}

private fun classpathsOf(prefix: String): List<String> =
    listOf("$prefix$COMPILE_CLASSPATH_SUFFIX", "$prefix$RUNTIME_CLASSPATH_SUFFIX")

private fun Project.registerResolveAndLockAll(isLocked: (String) -> Boolean) {
    val isWriteDependencyLocks = gradle.startParameter.isWriteDependencyLocks

    tasks.register(DependencyLockingTaskNames.RESOLVE_AND_LOCK_ALL) {
        description = "Resolves the locked classpaths so that --write-locks records every one of them."
        group = KreateTaskGroup.LOCKING.label

        notCompatibleWithConfigurationCache("Resolves configurations at execution time.")

        doFirst {
            check(isWriteDependencyLocks) {
                "${DependencyLockingTaskNames.RESOLVE_AND_LOCK_ALL} only makes sense with " +
                    "--write-locks: ./gradlew ${DependencyLockingTaskNames.RESOLVE_AND_LOCK_ALL} " +
                    "--write-locks"
            }
        }

        doLast {
            val locked = project.configurations.filter { configuration ->
                configuration.isCanBeResolved && isLocked(configuration.name)
            }

            locked.forEach { configuration -> configuration.incoming.resolutionResult.root }
            logger.lifecycle(
                "Resolved ${locked.size} locked configuration(s): ${locked.joinToString { it.name }}"
            )
        }
    }
}
