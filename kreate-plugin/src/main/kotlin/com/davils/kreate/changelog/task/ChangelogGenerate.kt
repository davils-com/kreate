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
package com.davils.kreate.changelog.task

import com.davils.kreate.changelog.build.ChangelogBuilder
import com.davils.kreate.changelog.build.ChangelogChanges
import com.davils.kreate.changelog.build.ChangelogRenderer
import com.davils.kreate.changelog.commit.ConventionalCommitParser
import com.davils.kreate.changelog.document.ChangelogWriter
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import com.davils.kreate.tooling.ExecOperationsToolRunner
import com.davils.kreate.tooling.ExecutableResolver
import com.davils.kreate.tooling.ExternalTool
import com.davils.kreate.vcs.GitHistory
import com.davils.kreate.vcs.ReleaseTag
import com.davils.kreate.vcs.SemanticVersion
import com.davils.kreate.vcs.VersionBump
import com.davils.kreate.vcs.coreVersionOf
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

@UntrackedTask(because = "It reads the Git history and edits the changelog on request")
internal abstract class ChangelogGenerate @Inject constructor(private val exec: ExecOperations) : KreateTask(
    "Writes the section of the next release from the conventional commits since the last release tag.",
    KreateTaskGroup.KREATE
) {
    @get:Internal
    @get:Option(option = "release", description = "The version to release. Defaults to the suggested one.")
    public abstract val release: Property<String>

    @get:Internal
    @get:Option(option = "from", description = "The revision to start from. Defaults to the last release tag.")
    public abstract val from: Property<String>

    @get:Internal
    @get:Option(option = "write", description = "Inserts the section into the changelog.")
    public abstract val write: Property<Boolean>

    @get:Internal
    public abstract val changelog: RegularFileProperty

    @get:Internal
    public abstract val projectDirectory: DirectoryProperty

    @get:Internal
    public abstract val projectVersion: Property<String>

    @get:Internal
    public abstract val tagPrefix: Property<String>

    @get:Internal
    public abstract val includeDates: Property<Boolean>

    @get:Internal
    public abstract val outputFile: RegularFileProperty

    @TaskAction
    public fun generate() {
        val directory = projectDirectory.get().asFile
        val git = GitHistory(ExecOperationsToolRunner(exec), ExecutableResolver.resolve(ExternalTool.GIT), directory)
        val latest = git.latestTag(tagPrefix.get())
        val start = from.orNull ?: latest?.name
        val commits = git.commits(start).map(ConventionalCommitParser::parse)
        val changes = ChangelogBuilder.build(commits)
        val version = versionOf(latest, changes)
        val today = LocalDate.now(ZoneOffset.UTC)
        val date = today.toString().takeIf { includeDates.get() }
        val section = ChangelogRenderer.render(version.toString(), date, changes)
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(section)
        logger.lifecycle(section)
        if (write.getOrElse(false)) insert(version.toString(), section)
    }

    private fun versionOf(latest: ReleaseTag?, changes: ChangelogChanges): SemanticVersion {
        val requested = release.orNull
        if (requested != null) {
            return SemanticVersion.parse(requested)
                ?: throw InvalidUserDataException("'$requested' is no semantic version such as 1.2.3.")
        }
        val declared = coreVersionOf(projectVersion.orNull)
        if (latest == null) {
            return declared ?: throw InvalidUserDataException("No release tag yet; name the version with --release.")
        }
        val next = (changes.bump ?: VersionBump.PATCH).apply(latest.version)
        if (declared != null && declared > next) return declared
        return next
    }

    private fun insert(version: String, section: String) {
        val file = changelog.get().asFile
        val existing = file.takeIf { candidate -> candidate.isFile }
        val text = existing?.readText().orEmpty()
        val updated = try {
            ChangelogWriter.insert(text, version, section)
        } catch (cause: IllegalArgumentException) {
            throw InvalidUserDataException(cause.message.orEmpty(), cause)
        }
        file.writeText(updated)
        logger.lifecycle("Inserted the section of $version into ${file.name}.")
    }
}
