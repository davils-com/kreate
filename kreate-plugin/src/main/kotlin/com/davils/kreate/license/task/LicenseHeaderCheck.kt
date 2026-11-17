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

package com.davils.kreate.license.task

import com.davils.kreate.license.header.CommentStyle
import com.davils.kreate.license.header.LicenseHeader
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationException
import java.io.File

@CacheableTask
internal abstract class LicenseHeaderCheck : KreateTask(
    "Checks that every source, build script and configuration file starts with the license header.",
    KreateTaskGroup.KREATE
) {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val sources: ConfigurableFileCollection

    @get:Input
    public abstract val notice: Property<String>

    @get:Internal
    public abstract val baseDirectory: DirectoryProperty

    @get:OutputFile
    public abstract val reportFile: RegularFileProperty

    @TaskAction
    public fun check() {
        val header = LicenseHeader(notice.get())
        if (header.isEmpty) throw VerificationException("The license header notice is empty.")
        val base = baseDirectory.get().asFile
        val relative = sources.files.map { file -> file.relativeTo(base) }
        val checked = relative.sortedBy { file -> file.invariantSeparatorsPath }
        val offending = checked.filterNot { file -> carriesHeader(base.resolve(file), header) }
        val report = reportFile.get().asFile
        report.parentFile.mkdirs()
        val lines = checked.map { file -> lineOf(file, file in offending) }
        report.writeText(lines.joinToString("\n", postfix = "\n"))
        if (offending.isEmpty()) return logger.lifecycle("${checked.size} file(s) start with the license header.")
        val names = offending.joinToString("\n") { file -> "  ${file.invariantSeparatorsPath}" }
        throw VerificationException("${offending.size} file(s) do not start with the license header:\n$names")
    }

    private fun carriesHeader(file: File, header: LicenseHeader): Boolean {
        val style = CommentStyle.forExtension(file.extension)
        return header.isPresentIn(file.readText(), style)
    }

    private fun lineOf(file: File, isOffending: Boolean): String {
        val verdict = when (isOffending) {
            true -> "missing"
            false -> "present"
        }
        return "$verdict ${file.invariantSeparatorsPath}"
    }
}
