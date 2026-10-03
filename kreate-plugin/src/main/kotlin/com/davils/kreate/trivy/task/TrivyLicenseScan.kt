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

import com.davils.kreate.trivy.lockfile.retainConfigurations
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

private const val LINE_SEPARATOR: String = "\n"

@DisableCachingByDefault(because = "Trivy scans depend on external vulnerability databases and tools")
internal abstract class TrivyLicenseScan @Inject constructor(
    exec: ExecOperations
) : TrivyLockFileScan(
    "Scans source files for licenses using Trivy",
    "license",
    "license",
    "Trivy found forbidden or restricted licenses in dependencies!",
    exec
) {
    @get:Input
    public abstract val ignoredLicenses: ListProperty<String>

    @get:Input
    public abstract val configurations: SetProperty<String>

    override fun scanTarget(lockFile: File): File {
        val lines = lockFile.readLines()
        val retained = retainConfigurations(lines, configurations.get())
        val directoryName = Integer.toHexString(lockFile.absolutePath.hashCode())
        val directory = temporaryDir.resolve(directoryName)
        directory.mkdirs()
        val filtered = directory.resolve(lockFile.name)
        filtered.writeText(retained.joinToString(LINE_SEPARATOR, postfix = LINE_SEPARATOR))
        return filtered
    }

    override fun scannerArguments(): List<String> {
        val ignored = ignoredLicenses.get()
        if (ignored.isEmpty()) return emptyList()
        return listOf("--ignored-licenses", joinForTrivy(ignored))
    }
}
