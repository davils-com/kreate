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

package com.davils.kreate.cinterop.task

import com.davils.kreate.cinterop.rust.resolveRustTargets
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import com.davils.kreate.tooling.ExecutableResolver
import com.davils.kreate.tooling.ExternalTool
import com.davils.kreate.tooling.runExternalTool
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

private const val CARGO_TARGET_DIR_NAME: String = "target"

@DisableCachingByDefault(because = "Rust compilation depends on external environment and tools")
internal abstract class CompileRust @Inject constructor(
    private val exec: ExecOperations
) : KreateTask("Compile Rust code for C interop", KreateTaskGroup.C_INTEROP) {
    @get:Internal
    public abstract val workDir: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val nativeSources: ConfigurableFileCollection

    @get:Input
    @get:Optional
    public abstract val rustTargets: ListProperty<String>

    @get:OutputDirectory
    public val outputDir: File
        get() = workDir.get().asFile.resolve(CARGO_TARGET_DIR_NAME)

    @TaskAction
    public fun execute() {
        val targets = resolveRustTargets(rustTargets.getOrElse(emptyList()))
        val cargoCmd = ExecutableResolver.resolve(ExternalTool.CARGO)

        for (target in targets) {
            runExternalTool(
                exec = exec,
                logger = logger,
                description = "Cargo build for target '$target'",
                workingDirectory = workDir.get().asFile,
                arguments = listOf(cargoCmd, "build", "--target", target, "--release")
            )
        }
    }
}
