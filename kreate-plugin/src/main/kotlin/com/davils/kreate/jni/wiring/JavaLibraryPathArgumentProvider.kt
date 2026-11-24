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

package com.davils.kreate.jni.wiring

import com.davils.kreate.jni.task.JNI_USE_LIBRARY_PATH_PROPERTY
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider
import java.io.File

internal class JavaLibraryPathArgumentProvider(
    @get:Input
    @get:Optional
    val libraryPath: Provider<String>,
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    val dependencyLibraries: FileCollection
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> {
        val own = libraryPath.orNull?.takeIf { path -> path.isNotEmpty() }
        val dependencies = dependencyLibraries.files.map { directory -> directory.absolutePath }
        val paths = (listOfNotNull(own) + dependencies).distinct()
        if (paths.isEmpty()) return emptyList()

        return listOf(
            "-Djava.library.path=${paths.joinToString(File.pathSeparator)}",
            "-D$JNI_USE_LIBRARY_PATH_PROPERTY=true"
        )
    }
}
