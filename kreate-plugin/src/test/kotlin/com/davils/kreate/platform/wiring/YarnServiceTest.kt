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

package com.davils.kreate.platform.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreatePlugin
import com.davils.kreate.freshDirectory
import com.davils.kreate.platform.WebEnvironment
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.TaskInternal
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

private val NODE: Set<WebEnvironment> = setOf(WebEnvironment.NODE)

private val YARN_TASKS: List<String> = listOf("kotlinNpmInstall", "kotlinWasmNpmInstall", "kotlinWasmToolingSetup")

private fun projectWithWebTargets(workspace: File): Project {
    val builder = ProjectBuilder.builder()
    builder.withProjectDir(workspace.freshDirectory("project"))
    builder.withGradleUserHomeDir(workspace.freshDirectory("gradle-home"))
    val project = builder.build()
    project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
    project.pluginManager.apply(KreatePlugin::class.java)
    val multiplatform = project.extensions.getByType(KreateExtension::class.java).platform.multiplatform
    multiplatform.targets {
        js.set(NODE)
        wasmJs.set(NODE)
    }

    (project as ProjectInternal).evaluate()
    return project
}

private fun servicesOf(task: Task): List<Any> {
    val required = (task as TaskInternal).requiredServices
    return required.elements.map { service -> service.get() }
}

class YarnServiceTest : FunSpec({
    val workspace = tempdir()

    context("YarnService") {
        test("admits one Yarn process at a time") {
            val project = projectWithWebTargets(workspace)
            project.tasks.getByName(YARN_TASKS.first())

            val registrations = project.gradle.sharedServices.registrations
            registrations.getByName(YARN_SERVICE).maxParallelUsages.get() shouldBe 1
        }

        test("guards every task that runs Yarn for the JavaScript and the Wasm toolchain") {
            val project = projectWithWebTargets(workspace)
            val yarnTasks = YARN_TASKS.map { name -> project.tasks.getByName(name) }
            val registration = project.gradle.sharedServices.registrations.findByName(YARN_SERVICE)

            registration shouldNotBe null
            val yarn = registration?.service?.get()
            yarnTasks.forEach { task -> servicesOf(task) shouldContain yarn }
        }
    }
})
