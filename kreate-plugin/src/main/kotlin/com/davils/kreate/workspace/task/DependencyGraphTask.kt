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
import com.davils.kreate.dependency.graph.GraphEdge
import com.davils.kreate.dependency.graph.GraphFormat
import com.davils.kreate.dependency.graph.GraphRenderer
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Drawing a graph is cheaper than storing it")
internal abstract class DependencyGraphTask : KreateTask(
    "Draws a dependency graph as text, DOT or Mermaid.",
    KreateTaskGroup.ASSURANCE
) {
    @get:Input
    @get:Option(option = "format", description = "text, dot or mermaid. Defaults to text.")
    public abstract val format: Property<String>

    @get:Input
    public abstract val graphs: ListProperty<DependencyGraphModel>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val descriptors: ConfigurableFileCollection

    @get:Input
    public abstract val fileName: Property<String>

    @get:OutputDirectory
    public abstract val outputDirectory: DirectoryProperty

    @TaskAction
    public fun draw() {
        val requested = format.get()
        val graphFormat = GraphFormat.parse(requested)
            ?: throw InvalidUserDataException("'$requested' is no graph format; use text, dot or mermaid.")
        val text = GraphRenderer.render(graphs.get() + workspaceGraphsOf(descriptors), graphFormat)
        val file = outputDirectory.get().file("${fileName.get()}.${graphFormat.extension}").asFile
        file.parentFile.mkdirs()
        file.writeText(text)
        if (graphFormat == GraphFormat.TEXT) logger.lifecycle(text)
        logger.lifecycle("Wrote ${file.path}.")
    }
}

internal fun workspaceGraphsOf(files: Iterable<File>): List<DependencyGraphModel> {
    val members = membersOf(files).sortedBy { member -> member.path }
    return members.map { member ->
        val edges = member.projects.map { target -> GraphEdge(member.path, target) }
        DependencyGraphModel(member.path, member.path, edges)
    }
}
