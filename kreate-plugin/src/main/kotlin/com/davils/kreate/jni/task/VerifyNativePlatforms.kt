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

package com.davils.kreate.jni.task

import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

internal const val PLATFORM_CANDIDATE_SEPARATOR: Char = '='

@DisableCachingByDefault(because = "Verification is cheap and depends on directory contents")
internal abstract class VerifyNativePlatforms : KreateTask(
    "Checks that every platform selected for publishing has a native library.",
    KreateTaskGroup.JNI
) {
    @get:Input
    public abstract val platforms: ListProperty<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val searchedDirectories: ConfigurableFileCollection

    @get:Input
    public abstract val candidatePaths: ListProperty<String>

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

        val candidates = candidatePaths.get().map { entry -> parseCandidateEntry(entry) }
        val missing = candidates.filter { (_, paths) -> paths.all { path -> isEmptyDirectory(path) } }

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

    private fun parseCandidateEntry(entry: String): Pair<String, List<String>> {
        val (platform, paths) = entry.split(PLATFORM_CANDIDATE_SEPARATOR, limit = 2)
        return platform to paths.split(File.pathSeparator)
    }

    private fun isEmptyDirectory(path: String): Boolean {
        val entries = File(path).listFiles()
        return entries.isNullOrEmpty()
    }
}
