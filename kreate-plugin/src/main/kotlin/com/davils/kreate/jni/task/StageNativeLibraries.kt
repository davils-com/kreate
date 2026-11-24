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

import com.davils.kreate.jni.stage.LibraryDigest
import com.davils.kreate.jni.stage.STAGE_MANIFEST_FILE_NAME
import com.davils.kreate.jni.stage.StageManifest
import com.davils.kreate.jni.stage.StageManifestFormat
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

@UntrackedTask(because = "The manifest names the commit of the build, which is no input Gradle can track")
internal abstract class StageNativeLibraries @Inject constructor(
    private val exec: ExecOperations
) : KreateTask(
    "Stages the native library of this platform with a manifest, for a publishing build to collect.",
    KreateTaskGroup.JNI
) {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val libraryDirectory: DirectoryProperty

    @get:Input
    public abstract val platformId: Property<String>

    @get:Input
    public abstract val artifactGroup: Property<String>

    @get:Input
    public abstract val artifactName: Property<String>

    @get:Input
    public abstract val artifactVersion: Property<String>

    @get:Internal
    public abstract val projectDirectory: DirectoryProperty

    @get:OutputDirectory
    public abstract val stageDirectory: DirectoryProperty

    @TaskAction
    public fun execute() {
        val libraries = librariesToStage()
        val target = stageDirectory.get().asFile
        target.deleteRecursively()
        target.mkdirs()

        libraries.forEach { library -> library.copyTo(target.resolve(library.name)) }
        val manifest = StageManifest(
            platform = platformId.get(),
            group = artifactGroup.get(),
            artifact = artifactName.get(),
            version = artifactVersion.get(),
            revision = headRevisionOrNull(exec, projectDirectory.get().asFile, logger),
            digests = libraries.associate { library -> library.name to LibraryDigest.sha256Of(library) }
        )
        target.resolve(STAGE_MANIFEST_FILE_NAME).writeText(StageManifestFormat.render(manifest))
        logger.lifecycle("Staged ${libraries.size} native file(s) for ${platformId.get()} in ${target.absolutePath}")
    }

    private fun librariesToStage(): List<File> {
        val source = libraryDirectory.get().asFile
        val listed = source.listFiles().orEmpty()
        val entries = listed.toList()
        val nested = entries.filter { entry -> entry.isDirectory }
        if (nested.isNotEmpty()) throw nestedDirectoryFailure(source, nested)

        val libraries = entries.filter { entry -> entry.isFile }
        requireStageable(source, libraries)
        return libraries.sortedBy { library -> library.name }
    }

    private fun requireStageable(source: File, libraries: List<File>) {
        if (libraries.isEmpty()) {
            throw GradleException("The native build left nothing to stage in ${source.absolutePath}.")
        }

        val unnamed = libraries.filterNot { library -> StageManifestFormat.isLibraryName(library.name) }
        if (unnamed.isNotEmpty()) throw unnamedLibraryFailure(unnamed)
    }

    private fun nestedDirectoryFailure(source: File, nested: List<File>): GradleException = GradleException(
        "The native build wrote directories into ${source.absolutePath}: " +
            "${nested.joinToString { directory -> directory.name }}. Only the files directly in it are " +
            "published, so a library in a subdirectory would be missing from the platform artifact."
    )

    private fun unnamedLibraryFailure(unnamed: List<File>): GradleException = GradleException(
        "These native files have names Kreate does not stage: " +
            "${unnamed.joinToString { library -> library.name }}. " +
            "A staged file name uses letters, digits, '.', '_', '+' and '-' only."
    )
}
