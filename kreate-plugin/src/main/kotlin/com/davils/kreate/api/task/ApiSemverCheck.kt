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
package com.davils.kreate.api.task

import com.davils.kreate.api.semver.ApiChangeKind
import com.davils.kreate.api.semver.SemverEvaluator
import com.davils.kreate.api.semver.SemverResult
import com.davils.kreate.changelog.commit.ConventionalCommitParser
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import com.davils.kreate.tooling.ExecOperationsToolRunner
import com.davils.kreate.tooling.ExecutableResolver
import com.davils.kreate.tooling.ExternalTool
import com.davils.kreate.vcs.GitHistory
import com.davils.kreate.vcs.ReleaseTag
import com.davils.kreate.vcs.SemanticVersion
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.api.tasks.VerificationException
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

@UntrackedTask(because = "It compares with the Git history")
internal abstract class ApiSemverCheck @Inject constructor(private val exec: ExecOperations) : KreateTask(
    "Checks that the project version is high enough for how the API changed since the last release.",
    KreateTaskGroup.KREATE
) {
    @get:Internal
    public abstract val dumpFile: RegularFileProperty

    @get:Internal
    public abstract val projectDirectory: DirectoryProperty

    @get:Internal
    public abstract val projectVersion: Property<String>

    @get:Internal
    public abstract val baseline: Property<String>

    @get:Internal
    public abstract val tagPrefix: Property<String>

    @get:Internal
    public abstract val requireBreakingCommit: Property<Boolean>

    @get:Internal
    public abstract val reportFile: RegularFileProperty

    @TaskAction
    public fun check() {
        val git = GitHistory(ExecOperationsToolRunner(exec), ExecutableResolver.resolve(ExternalTool.GIT), directory())
        if (!git.isRepository()) return skip("the project is not inside a Git repository")
        val tag = baselineOf(git) ?: return skip("there is no release tag with the prefix '${tagPrefix.get()}' yet")
        val dump = existingDump()
        val path = relativePath(git.root(), dump)
        val before = git.show(tag.name, path) ?: return skip("${tag.name} has no API dump at $path")
        val version = declaredVersion()
        val announced = breakingAnnounced(git, tag)
        val result = SemverEvaluator.evaluate(before, dump.readText(), tag.name, tag.version, version, announced)
        report(result)
        if (!result.isValid) throw VerificationException(result.problems.joinToString("\n"))
    }

    private fun directory(): File = projectDirectory.get().asFile

    private fun existingDump(): File {
        val dump = dumpFile.get().asFile
        if (!dump.isFile) throw InvalidUserDataException("${dump.path} does not exist. Run kreateApiDump first.")
        return dump
    }

    private fun relativePath(root: File, file: File): String {
        val rootPath = root.toPath().toRealPath()
        val relative = rootPath.relativize(file.toPath().toRealPath())
        return relative.joinToString("/")
    }

    private fun declaredVersion(): SemanticVersion {
        val declared = projectVersion.get()
        return SemanticVersion.parse(declared)
            ?: throw InvalidUserDataException("The version '$declared' is no semantic version.")
    }

    private fun baselineOf(git: GitHistory): ReleaseTag? {
        val explicit = baseline.orNull ?: return git.latestTag(tagPrefix.get())
        val version = ReleaseTag.parse(explicit, tagPrefix.get())?.version ?: SemanticVersion.parse(explicit)
            ?: throw InvalidUserDataException("The baseline '$explicit' names no semantic version.")
        return ReleaseTag(explicit, version)
    }

    private fun breakingAnnounced(git: GitHistory, tag: ReleaseTag): Boolean? {
        if (!requireBreakingCommit.get()) return null
        val commits = git.commits(tag.name).map(ConventionalCommitParser::parse)
        return commits.any { commit -> commit.breaking }
    }

    private fun report(result: SemverResult) {
        val ordered = result.changes.sortedBy { change -> change.kind != ApiChangeKind.BREAKING }
        val lines = ordered.map { change -> "${change.kind.name.lowercase()}: ${change.description}" }
        val summary = "${result.changes.size} API change(s) since ${result.baseline} (${result.baselineVersion}); " +
            "they need a ${result.requiredBump.name.lowercase()} release, the version is ${result.version}."
        val file = reportFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText((listOf(summary) + lines + result.problems).joinToString("\n", postfix = "\n"))
        logger.lifecycle(summary)
        lines.take(MAX_LOGGED_CHANGES).forEach { line -> logger.lifecycle("  $line") }
    }

    private fun skip(reason: String) {
        logger.lifecycle("The semantic versioning check is skipped: $reason.")
    }

    private companion object {
        const val MAX_LOGGED_CHANGES: Int = 40
    }
}
