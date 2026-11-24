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

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

internal const val JNI_HOST_LIBRARY_USAGE: String = "kreate-jni-host-library"

private const val HOST_LIBRARY_ELEMENTS: String = "kreateJniHostLibraryElements"

internal fun Project.offerHostLibrary(libraryDirectory: Provider<out Directory>, buildTaskName: String) {
    val usage = objects.named(Usage::class.java, JNI_HOST_LIBRARY_USAGE)
    val category = objects.named(Category::class.java, Category.LIBRARY)

    configurations.consumable(HOST_LIBRARY_ELEMENTS) {
        description = "The native library this JNI project builds for the host, for the projects of the same build."
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, usage)
            attribute(Category.CATEGORY_ATTRIBUTE, category)
        }
        outgoing.artifact(libraryDirectory) {
            type = ArtifactTypeDefinition.DIRECTORY_TYPE
            builtBy(buildTaskName)
        }
    }
}

internal fun Project.dependencyHostLibraries(): FileCollection {
    val usage = objects.named(Usage::class.java, JNI_HOST_LIBRARY_USAGE)
    val category = objects.named(Category::class.java, Category.LIBRARY)
    val views = provider {
        val classpaths = jvmRuntimeClasspathNames().mapNotNull { name -> configurations.findByName(name) }
        classpaths.map { classpath -> classpath.hostLibraries(usage, category) }
    }
    return files(views)
}

private fun Configuration.hostLibraries(usage: Usage, category: Category): FileCollection {
    val view = incoming.artifactView {
        withVariantReselection()
        lenient(true)
        componentFilter { component -> component is ProjectComponentIdentifier }
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, usage)
            attribute(Category.CATEGORY_ATTRIBUTE, category)
        }
    }
    return view.files
}

private fun Project.jvmRuntimeClasspathNames(): List<String> {
    val sourceSets = extensions.findByType(SourceSetContainer::class.java)
    val sourceSetClasspaths = sourceSets?.map { sourceSet -> sourceSet.runtimeClasspathConfigurationName }.orEmpty()

    val multiplatform = extensions.findByType(KotlinMultiplatformExtension::class.java)
    val jvmTargets = multiplatform?.targets?.withType(KotlinJvmTarget::class.java).orEmpty()
    val compilationClasspaths = jvmTargets.flatMap { target ->
        target.compilations.mapNotNull { compilation -> compilation.runtimeDependencyConfigurationName }
    }

    return (sourceSetClasspaths + compilationClasspaths).distinct()
}
