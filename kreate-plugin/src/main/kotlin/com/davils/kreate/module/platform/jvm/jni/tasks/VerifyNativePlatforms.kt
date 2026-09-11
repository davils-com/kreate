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

package com.davils.kreate.module.platform.jvm.jni.tasks

import com.davils.kreate.jobs.Task
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Fails the build when a platform selected for publishing has no native library to publish.
 *
 * Publishing a subset of platforms is a supported state, so this task says nothing about the
 * platforms a release leaves out. It only checks the gap between what was selected and what
 * exists — a gap that is always an accident, and one that is otherwise invisible: a release with
 * a missing binary uploads cleanly, and the defect surfaces later as an `UnsatisfiedLinkError` in
 * a consumer's process.
 *
 * @since 2.2.0
 */
@DisableCachingByDefault(because = "Verification is cheap and depends on directory contents")
public abstract class VerifyNativePlatforms : Task(
    "Checks that every platform selected for publishing has a native library.",
    "kreate jni"
) {
    /**
     * The platform identifiers selected for publishing.
     *
     * @since 2.2.0
     */
    @get:Input
    public abstract val platforms: ListProperty<String>

    /**
     * The directories searched for each platform, in the order they are searched.
     *
     * Declared as an input so that adding a staged binary re-runs the check.
     *
     * @since 2.2.0
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val searchedDirectories: ConfigurableFileCollection

    /**
     * The candidate directories per platform, used to report where the task looked.
     *
     * @since 2.2.0
     */
    @get:Input
    public abstract val candidatePaths: ListProperty<String>

    /**
     * Verifies the selection against what is on disk.
     *
     * @throws GradleException If a selected platform has no library in any candidate directory.
     * @since 2.2.0
     */
    @TaskAction
    public fun execute() {
        val selected = platforms.get()
        if (selected.isEmpty()) {
            throw GradleException(
                "Kreate's native publishing is enabled but no platform is selected. Set " +
                    "`jni { packaging { publishing { platforms = listOf(\"linux-x86_64\") } } }`, " +
                    "or pass -Pkreate.jni.publishPlatforms=<ids>."
            )
        }

        val candidates = candidatePaths.get().map { entry ->
            val (platform, paths) = entry.split('=', limit = 2)
            platform to paths.split(java.io.File.pathSeparator)
        }

        val missing = candidates.filter { (_, paths) ->
            paths.all { path -> java.io.File(path).listFiles().isNullOrEmpty() }
        }

        if (missing.isEmpty()) {
            logger.lifecycle("Native libraries present for: ${selected.joinToString(", ")}")
            return
        }

        val detail = missing.joinToString(separator = "\n\n") { (platform, paths) ->
            "  $platform — looked in:\n" + paths.joinToString("\n") { path -> "    $path" }
        }

        throw GradleException(
            """
                No native library was found for every platform selected for publishing.

                $detail

                Either build or stage a binary for the platforms above, or drop them from
                `jni { packaging { publishing { platforms } } }`. Publishing a subset is
                supported; publishing a platform that has no binary is not, because the release
                would upload cleanly and fail in a consumer's process instead.
            """.trimIndent()
        )
    }
}
