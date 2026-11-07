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
package com.davils.kreate.assurance.task

import com.davils.kreate.assurance.config.ConfigRenderer
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "It only prints the configuration")
internal abstract class AssuranceConfigShow : KreateTask(
    "Shows the effective assurance configuration; * marks what differs from the defaults.",
    KreateTaskGroup.ASSURANCE
) {
    @get:Input
    public abstract val values: MapProperty<String, String>

    @get:Input
    public abstract val defaults: MapProperty<String, String>

    @get:OutputFile
    public abstract val outputFile: RegularFileProperty

    @TaskAction
    public fun show() {
        val effective = values.get()
        val text = ConfigRenderer.render(effective, defaults.get())
        val changed = ConfigRenderer.changes(effective, defaults.get())
        val json = JsonObject(
            mapOf(
                "values" to JsonObject(effective.mapValues { (_, value) -> JsonPrimitive(value) }),
                "changed" to JsonObject(changed.mapValues { (_, value) -> JsonPrimitive(value) })
            )
        )
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(json.toString())
        logger.lifecycle(text)
    }
}
