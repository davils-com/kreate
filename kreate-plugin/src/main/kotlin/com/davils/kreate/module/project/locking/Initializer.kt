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

package com.davils.kreate.module.project.locking

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreateTasks
import com.davils.kreate.module.project.tests.suite.enabledSuites
import com.davils.kreate.module.project.tests.suite.lowerCamelCaseName
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Initializes Gradle dependency locking for the project.
 *
 * Activates locking on the configured classpaths and registers
 * [KreateTasks.DependencyLocking.RESOLVE_AND_LOCK_ALL], which is the task that makes
 * `--write-locks` record every locked classpath in one run.
 *
 * @param extension The main Kreate extension.
 * @since 2.1.0
 */
internal fun Project.initializeDependencyLocking(extension: KreateExtension) {
    val lockingExtension = extension.project.dependencyLocking
    if (!lockingExtension.enabled.get()) return

    // Read once, at configuration time: the task action below must not reach back into the
    // extension, or the task would carry a reference to the whole project model.
    val lockedClasspaths = lockingExtension.lockedClasspaths.get() +
        multiplatformClasspaths() +
        testSuiteClasspaths(extension)

    val lockEverything = lockingExtension.lockAllConfigurations.get()
    if (lockEverything) {
        dependencyLocking.lockAllConfigurations()
    } else {
        configurations.matching { it.name in lockedClasspaths }.configureEach {
            resolutionStrategy.activateDependencyLocking()
        }
    }

    registerResolveAndLockAll(if (lockEverything) null else lockedClasspaths)
}

/**
 * The classpaths of every declared Kotlin target, or an empty set for a project that is not
 * multiplatform.
 *
 * The default classpath names are the ones the Kotlin JVM plugin uses. The multiplatform plugin
 * names a classpath after its target instead - `jvmRuntimeClasspath`, `androidCompileClasspath`,
 * `wasmJsCompileClasspath` - so on a multiplatform project the default matches nothing, locking
 * silently applies to no configuration, and any `gradle.lockfile` left in the repository from
 * before is never rewritten. It still looks like a lock file to a reader and to Trivy.
 *
 * Both directions are derived for every target, including the metadata one, whose compile classpath
 * is `metadataCompileClasspath`. A derived name that turns out not to exist is harmless: locking
 * matches configurations by name and simply never matches it.
 *
 * @return The per-target classpath names to lock in addition to the configured ones.
 * @since 2.3.0
 */
private fun Project.multiplatformClasspaths(): Set<String> {
    val multiplatform = extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return emptySet()

    return multiplatform.targets.flatMapTo(mutableSetOf()) { target ->
        listOf("${target.name}CompileClasspath", "${target.name}RuntimeClasspath")
    }
}

/**
 * The classpaths of every enabled test suite.
 *
 * A suite brings dependencies of its own - an integration suite is usually the only place a
 * project declares Testcontainers and a database driver - and those classpaths carry names the
 * defaults do not match. Left out, the lock file would be missing precisely the dependencies
 * that reach outside the process, while still looking complete to a reader and to Trivy.
 *
 * Derived from the configuration rather than read off the created configurations, because
 * locking is initialised before the suites exist. That is harmless: locking matches
 * configurations by name as they are created, and a name that never appears never matches.
 *
 * @param extension The main Kreate extension.
 * @return The suite classpath names to lock in addition to the configured ones.
 * @since 3.0.0
 */
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

        prefixes.flatMap { prefix -> listOf("${prefix}CompileClasspath", "${prefix}RuntimeClasspath") }
    }
}

/**
 * Registers the task that resolves every locked classpath.
 *
 * `--write-locks` only records the configurations a build actually resolves, so running it
 * against an arbitrary task writes a partial lock file — one that looks complete and is
 * not. This task exists to resolve all of them in a single invocation.
 *
 * Unlike every other Kreate task this one is a plain `DefaultTask` registered by name
 * rather than a subclass of [com.davils.kreate.jobs.Task]. Resolving a configuration is
 * inherently an execution-time interaction with the project model, which no typed task
 * with declared inputs and outputs can express; `notCompatibleWithConfigurationCache` is
 * the sanctioned way to say so, and it must not be removed in a later cleanup.
 *
 * @param lockedClasspaths The names of the configurations to resolve, or `null` to resolve
 *   every resolvable configuration because all of them are locked.
 * @since 2.1.0
 */
private fun Project.registerResolveAndLockAll(lockedClasspaths: Set<String>?) {
    val isWriteDependencyLocks = gradle.startParameter.isWriteDependencyLocks

    tasks.register(KreateTasks.DependencyLocking.RESOLVE_AND_LOCK_ALL) {
        description = "Resolves the locked classpaths so that --write-locks records every one of them."
        group = KreateTasks.DependencyLocking.GROUP

        notCompatibleWithConfigurationCache("Resolves configurations at execution time.")

        doFirst {
            check(isWriteDependencyLocks) {
                "${KreateTasks.DependencyLocking.RESOLVE_AND_LOCK_ALL} only makes sense with " +
                    "--write-locks: ./gradlew ${KreateTasks.DependencyLocking.RESOLVE_AND_LOCK_ALL} " +
                    "--write-locks"
            }
        }

        doLast {
            val locked = project.configurations.filter { configuration ->
                configuration.isCanBeResolved &&
                    (lockedClasspaths == null || configuration.name in lockedClasspaths)
            }

            locked.forEach { configuration -> configuration.incoming.resolutionResult.root }
            logger.lifecycle(
                "Resolved ${locked.size} locked configuration(s): ${locked.joinToString { it.name }}"
            )
        }
    }
}
