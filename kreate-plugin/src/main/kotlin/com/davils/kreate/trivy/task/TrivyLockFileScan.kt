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

package com.davils.kreate.trivy.task

import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Trivy scans depend on external vulnerability databases and tools")
internal abstract class TrivyLockFileScan(
    taskDescription: String,
    scanner: String,
    private val scanName: String,
    private val findingsMessage: String,
    exec: ExecOperations
) : TrivyScan(taskDescription, scanner, exec) {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val lockFiles: ConfigurableFileCollection

    @get:Input
    public abstract val failOnMissingLockFiles: Property<Boolean>

    protected open fun scanTarget(lockFile: File): File = lockFile

    @TaskAction
    public fun execute() {
        if (lockFiles.isEmpty) {
            handleMissingLockFiles()
            publish(emptyList()) { findingsMessage }
            return
        }

        val runs = lockFiles.files.map { lockFile -> runTrivy(targetOf(lockFile)) }
        publish(runs) { findingsMessage }
    }

    private fun targetOf(lockFile: File): TrivyTarget {
        val scanned = scanTarget(lockFile)
        return TrivyTarget(scanned) { lockFile.name }
    }

    private fun handleMissingLockFiles() {
        if (failOnMissingLockFiles.get()) {
            throw GradleException(
                "No lock files found, so the $scanName scan cannot check anything. Enable " +
                    "project { dependencyLocking { enabled = true } } and run " +
                    "'./gradlew kreateResolveAndLockAll --write-locks'."
            )
        }

        logger.lifecycle("No lock files found. Skipping $scanName scan.")
    }
}
