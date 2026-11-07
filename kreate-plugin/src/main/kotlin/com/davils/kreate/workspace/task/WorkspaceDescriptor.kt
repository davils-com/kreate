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
package com.davils.kreate.workspace.task

import com.davils.kreate.dependency.graph.DependencyGraphModel
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import com.davils.kreate.workspace.model.WorkspaceMember
import com.davils.kreate.workspace.model.WorkspaceMemberJson
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
internal abstract class WorkspaceDescriptor : KreateTask(
    "Describes the dependencies this project shares with the workspace.",
    KreateTaskGroup.ASSURANCE
) {
    @get:Input
    public abstract val projectPath: Property<String>

    @get:Input
    public abstract val directory: Property<String>

    @get:Input
    public abstract val graphs: ListProperty<DependencyGraphModel>

    @get:OutputFile
    public abstract val outputFile: RegularFileProperty

    @TaskAction
    public fun describe() {
        val self = projectPath.get()
        val nodes = graphs.get().flatMap { graph -> graph.nodes }
        val projects = nodes.filter { node -> node.startsWith(":") && node != self }.toSet()
        val external = nodes.filterNot { node -> node.startsWith(":") }
        val modules = external.mapNotNull(::moduleOf).toMap()
        val member = WorkspaceMember(self, directory.get(), projects, modules)
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(WorkspaceMemberJson.render(member))
    }

    private fun moduleOf(node: String): Pair<String, String>? {
        val parts = node.split(':')
        if (parts.size != COORDINATE_PARTS) return null
        return "${parts[0]}:${parts[1]}" to parts[2]
    }

    private companion object {
        const val COORDINATE_PARTS: Int = 3
    }
}
