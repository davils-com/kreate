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

import com.davils.kreate.changelog.document.ChangelogDocument
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import com.davils.kreate.vcs.coreVersionOf
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.api.tasks.options.Option
import java.io.File

@UntrackedTask(because = "It is run on request for a release")
internal abstract class ChangelogNotes : KreateTask(
    "Writes the section of one release as release notes.",
    KreateTaskGroup.KREATE
) {
    @get:Internal
    @get:Option(option = "release", description = "The released version. Defaults to the project version.")
    public abstract val release: Property<String>

    @get:Internal
    public abstract val changelog: RegularFileProperty

    @get:Internal
    public abstract val projectVersion: Property<String>

    @get:Internal
    public abstract val outputFile: RegularFileProperty

    @TaskAction
    public fun notes() {
        val version = release.orNull ?: coreVersionOf(projectVersion.orNull)?.toString()
            ?: throw InvalidUserDataException("Name the version with --release.")
        val file = changelog.get().asFile
        val body = bodyOf(file, version)
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(body + "\n")
        logger.lifecycle("Wrote the notes of $version to ${output.path}.")
    }

    private fun bodyOf(file: File, version: String): String {
        if (!file.isFile) throw InvalidUserDataException("${file.path} does not exist.")
        val document = ChangelogDocument.parse(file.readText())
        val heading = document.find(version)
        val body = heading?.let(document::body).orEmpty()
        if (body.isEmpty()) throw InvalidUserDataException("${file.name} has no notes for $version.")
        return body
    }
}
