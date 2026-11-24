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

import com.davils.kreate.jni.stage.STAGE_MANIFEST_FILE_NAME
import com.davils.kreate.jni.stage.StageExpectation
import com.davils.kreate.jni.stage.StageManifestCheck
import com.davils.kreate.jni.stage.StageManifestException
import com.davils.kreate.jni.stage.StageManifestFormat
import com.davils.kreate.jni.stage.StageReview
import com.davils.kreate.publish.guard.SNAPSHOT_SUFFIX
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

internal const val PLATFORM_CANDIDATE_SEPARATOR: Char = '='

@DisableCachingByDefault(because = "Verification is cheap and depends on directory contents")
internal abstract class VerifyNativePlatforms @Inject constructor(
    private val exec: ExecOperations
) : KreateTask(
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

    @get:Input
    public abstract val stagedPaths: ListProperty<String>

    @get:Input
    public abstract val artifactGroup: Property<String>

    @get:Input
    public abstract val artifactName: Property<String>

    @get:Input
    public abstract val artifactVersion: Property<String>

    @get:Input
    public abstract val requiresMainPublication: Property<Boolean>

    @get:Input
    public abstract val mainPublications: ListProperty<String>

    @get:Internal
    public abstract val projectDirectory: DirectoryProperty

    init {
        stagedPaths.convention(emptyList())
        requiresMainPublication.convention(false)
        mainPublications.convention(emptyList())
    }

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

        requireMainPublication()
        requireEveryPlatform()
        reviewStagedPlatforms()
        logger.lifecycle("Native libraries present for: ${selected.joinToString(", ")}")
    }

    private fun requireMainPublication() {
        if (!requiresMainPublication.get()) return
        if (mainPublications.get().isNotEmpty()) return

        throw GradleException(
            """
                Kreate's per-platform native publishing is enabled, but the project has no
                publication for the library itself.

                Publishing only the platform artifacts would produce a release containing
                native binaries and no code to call them.

                Enable a publishing target so that the main artifact is published too:

                    kreate { project { publish { repositories {
                        gitlab { enabled = true }        // or
                        mavenCentral { enabled = true }  // or
                        maven { releases = "https://repo.example.com/releases" }
                    } } } }

                Or declare a MavenPublication of your own.
            """.trimIndent()
        )
    }

    private fun requireEveryPlatform() {
        val candidates = candidatePaths.get().map { entry -> parseCandidateEntry(entry) }
        val missing = candidates.filter { (_, paths) -> paths.all { path -> isEmptyDirectory(path) } }
        if (missing.isEmpty()) return

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

    private fun reviewStagedPlatforms() {
        val staged = stagedPaths.get().map { entry -> parseStagedEntry(entry) }
        val populated = staged.filterNot { (_, directory) -> isEmptyDirectory(directory.path) }
        if (populated.isEmpty()) return

        val revision = headRevisionOrNull(exec, projectDirectory.get().asFile, logger)
        val reviews = populated.map { (platform, directory) -> platform to reviewOf(platform, directory, revision) }
        reviews.forEach { (platform, review) -> review.warnings.forEach { warning -> warn(platform, warning) } }

        val failed = reviews.filter { (_, review) -> review.problems.isNotEmpty() }
        if (failed.isEmpty()) return

        val detail = failed.joinToString(separator = "\n") { (platform, review) ->
            review.problems.joinToString(separator = "\n") { problem -> "  $platform $problem." }
        }
        throw GradleException(
            """
                A staged native library does not belong to this build:

                $detail

                Stage the libraries again with `kreateJniStage` from the commit and version this
                build publishes. Publishing them would ship binaries nobody built for this release.
            """.trimIndent()
        )
    }

    private fun reviewOf(platform: String, directory: File, revision: String?): StageReview {
        val manifestFile = directory.resolve(STAGE_MANIFEST_FILE_NAME)
        if (!manifestFile.isFile) {
            val warning = "was staged without a $STAGE_MANIFEST_FILE_NAME, so Kreate cannot tell which build " +
                "produced it. Stage it with kreateJniStage"
            return StageReview(problems = emptyList(), warnings = listOf(warning))
        }

        val expectation = StageExpectation(
            platform = platform,
            group = artifactGroup.get(),
            artifact = artifactName.get(),
            version = artifactVersion.get(),
            revision = revision,
            isRelease = !artifactVersion.get().endsWith(SNAPSHOT_SUFFIX)
        )
        return try {
            val manifest = StageManifestFormat.parse(manifestFile.readText())
            StageManifestCheck.review(manifest, expectation, directory)
        } catch (exception: StageManifestException) {
            val problem = "has a $STAGE_MANIFEST_FILE_NAME that ${exception.message}"
            StageReview(problems = listOf(problem), warnings = emptyList())
        }
    }

    private fun warn(platform: String, warning: String) {
        logger.warn("The staged native library of {} {}.", platform, warning)
    }

    private fun parseCandidateEntry(entry: String): Pair<String, List<String>> {
        val (platform, paths) = entry.split(PLATFORM_CANDIDATE_SEPARATOR, limit = 2)
        return platform to paths.split(File.pathSeparator)
    }

    private fun parseStagedEntry(entry: String): Pair<String, File> {
        val (platform, path) = entry.split(PLATFORM_CANDIDATE_SEPARATOR, limit = 2)
        return platform to File(path)
    }

    private fun isEmptyDirectory(path: String): Boolean {
        val entries = File(path).listFiles()
        return entries.isNullOrEmpty()
    }
}
