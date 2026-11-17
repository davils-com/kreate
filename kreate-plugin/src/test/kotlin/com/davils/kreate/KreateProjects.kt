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

package com.davils.kreate

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

fun evaluatedKreateProject(directory: File, pluginId: String, configure: (KreateExtension) -> Unit): Project {
    val builder = ProjectBuilder.builder()
    builder.withProjectDir(directory)
    val project = builder.build()
    project.pluginManager.apply(pluginId)
    project.pluginManager.apply(KreatePlugin::class.java)
    configure(project.extensions.getByType(KreateExtension::class.java))
    (project as ProjectInternal).evaluate()
    return project
}
