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

import com.davils.kreate.api.abi.AbiFilterOptions
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
internal abstract class ApiDump : KreateTask(
    "Writes the public binary interface of this project to its .api dump.",
    KreateTaskGroup.API
) {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val classDirectories: ConfigurableFileCollection

    @get:Input
    public abstract val nonPublicMarkers: SetProperty<String>

    @get:Input
    public abstract val ignoredPackages: SetProperty<String>

    @get:Input
    public abstract val ignoredClasses: SetProperty<String>

    @get:OutputFile
    public abstract val dumpFile: RegularFileProperty

    @TaskAction
    public fun execute() {
        val options = AbiFilterOptions(
            nonPublicMarkers = nonPublicMarkers.get(),
            ignoredPackages = ignoredPackages.get(),
            ignoredClasses = ignoredClasses.get()
        )
        val rendered = renderAbiDump(classDirectories, options)

        val target = dumpFile.get().asFile
        target.parentFile.mkdirs()
        target.writeText(rendered)

        logger.lifecycle("Wrote the public binary interface to ${target.path}.")
    }
}
