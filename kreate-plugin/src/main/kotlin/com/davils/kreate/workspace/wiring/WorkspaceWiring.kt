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
package com.davils.kreate.workspace.wiring

import com.davils.kreate.assurance.AssuranceWorkspaceExtension
import com.davils.kreate.assurance.wiring.buildMembers
import com.davils.kreate.dependency.graph.DependencyGraphModel
import com.davils.kreate.dependency.wiring.dependencyGraphOf
import com.davils.kreate.trivy.wiring.productionConfigurations
import com.davils.kreate.workspace.WorkspaceTaskNames
import com.davils.kreate.workspace.task.DependencyGraphTask
import com.davils.kreate.workspace.task.WorkspaceAffected
import com.davils.kreate.workspace.task.WorkspaceCheck
import com.davils.kreate.workspace.task.WorkspaceDescriptor
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.kotlin.dsl.register

private const val DESCRIPTOR_TASK: String = "kreateWorkspaceDescriptor"

private const val ELEMENTS: String = "kreateWorkspaceElements"

private const val COLLECTED: String = "kreateWorkspace"

private const val USAGE: String = "kreate-workspace"

private const val DESCRIPTOR_FILE: String = "kreate/workspace/descriptor.json"

private const val WORKSPACE_DOCUMENT: String = "kreate/assurance/documents/workspace.json"

internal fun Project.registerWorkspaceMember(workspace: AssuranceWorkspaceExtension) {
    workspace.configurations.convention(provider { productionConfigurations() })
    val member = this
    val graphs = workspace.configurations.flatMap { names -> member.graphsOf(names) }
    val root = isolated.rootProject.projectDirectory.asFile.toPath()
    val relative = root.relativize(projectDir.toPath()).joinToString("/")

    val descriptor = tasks.register<WorkspaceDescriptor>(DESCRIPTOR_TASK) {
        projectPath.set(member.path)
        directory.set(relative)
        this.graphs.set(graphs)
        outputFile.set(member.layout.buildDirectory.file(DESCRIPTOR_FILE))
    }

    configurations.consumable(ELEMENTS) {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, member.objects.named(Usage::class.java, USAGE))
        outgoing.artifact(descriptor.flatMap { task -> task.outputFile })
    }

    tasks.register<DependencyGraphTask>(WorkspaceTaskNames.DEPENDENCY_GRAPH) {
        format.convention("text")
        this.graphs.set(graphs)
        fileName.set("graph")
        outputDirectory.set(member.layout.buildDirectory.dir("kreate/dependencies"))
    }
}

internal fun Project.registerWorkspaceRoot(workspace: AssuranceWorkspaceExtension) {
    workspace.members.convention(buildMembers())
    val owner = this
    val collected = configurations.resolvable(COLLECTED) {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, owner.objects.named(Usage::class.java, USAGE))
    }
    val others = workspace.members.map { paths -> paths.filter { path -> path != owner.path } }
    val projectDependencies = others.map { paths -> paths.map { path -> owner.projectDependency(path) } }
    collected.configure { dependencies.addAllLater(projectDependencies) }
    val view = collected.map { configuration -> configuration.lenientFiles() }
    val descriptorTask = tasks.named(DESCRIPTOR_TASK, WorkspaceDescriptor::class.java)
    val ownDescriptor = descriptorTask.flatMap { task -> task.outputFile }

    tasks.register<WorkspaceCheck>(WorkspaceTaskNames.CHECK) {
        descriptors.from(view, ownDescriptor)
        members.set(workspace.members)
        alignVersions.set(workspace.alignVersions)
        forbidden.set(workspace.forbidden)
        documentFile.set(owner.layout.buildDirectory.file(WORKSPACE_DOCUMENT))
    }

    tasks.register<WorkspaceAffected>(WorkspaceTaskNames.AFFECTED) {
        base.convention(workspace.affectedBase)
        descriptors.from(view, ownDescriptor)
        dependsOn(view, ownDescriptor)
        rootDirectory.set(owner.layout.projectDirectory)
        outputFile.set(owner.layout.buildDirectory.file("kreate/workspace/affected.txt"))
    }

    tasks.register<DependencyGraphTask>(WorkspaceTaskNames.GRAPH) {
        format.convention("text")
        descriptors.from(view, ownDescriptor)
        fileName.set("workspace")
        outputDirectory.set(owner.layout.buildDirectory.dir("kreate/workspace"))
    }
}

private fun Project.projectDependency(path: String): Dependency = dependencies.project(mapOf("path" to path))

private fun Configuration.lenientFiles(): FileCollection {
    val view = incoming.artifactView { lenient(true) }
    return view.files
}

private fun Project.graphsOf(names: Set<String>): ListProperty<DependencyGraphModel> {
    val merged = objects.listProperty(DependencyGraphModel::class.java)
    val named = configurations.filter { configuration -> configuration.name in names }
    val selected = named.filter { configuration -> configuration.isCanBeResolved }
    selected.forEach { configuration ->
        val root = configuration.incoming.resolutionResult.rootComponent
        merged.add(root.map { component -> dependencyGraphOf(configuration.name, component) })
    }
    return merged
}
