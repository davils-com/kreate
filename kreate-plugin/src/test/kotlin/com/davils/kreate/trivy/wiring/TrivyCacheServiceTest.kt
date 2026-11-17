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
package com.davils.kreate.trivy.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreatePlugin
import com.davils.kreate.freshDirectory
import com.davils.kreate.trivy.TrivyTaskNames
import com.davils.kreate.trivy.task.TrivyScan
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.gradle.api.Project
import org.gradle.api.internal.TaskInternal
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

private fun projectWithEveryScan(workspace: File): Project {
    val builder = ProjectBuilder.builder()
    builder.withProjectDir(workspace.freshDirectory("project"))
    builder.withGradleUserHomeDir(workspace.freshDirectory("gradle-home"))
    val project = builder.build()
    project.pluginManager.apply(KreatePlugin::class.java)
    val trivy = project.extensions.getByType(KreateExtension::class.java).trivy
    trivy.enabled.set(true)
    trivy.misconfig.enabled.set(true)
    trivy.filesystem.enabled.set(true)

    (project as ProjectInternal).evaluate()
    return project
}

private fun servicesOf(scan: TrivyScan): List<Any> {
    val required = (scan as TaskInternal).requiredServices
    return required.elements.map { service -> service.get() }
}

class TrivyCacheServiceTest : FunSpec({
    val workspace = tempdir()

    context("TrivyCacheService") {
        test("admits one Trivy scan at a time") {
            val project = projectWithEveryScan(workspace)
            project.tasks.getByName(TrivyTaskNames.VULNERABILITIES)

            val registrations = project.gradle.sharedServices.registrations
            registrations.getByName(TRIVY_CACHE_SERVICE).maxParallelUsages.get() shouldBe 1
        }

        test("guards every Trivy scan, so no new scan reads the cache of Trivy beside another") {
            val project = projectWithEveryScan(workspace)
            val scans = project.tasks.withType(TrivyScan::class.java).toList()
            val registration = project.gradle.sharedServices.registrations.findByName(TRIVY_CACHE_SERVICE)

            scans.map { scan -> scan.name } shouldContainAll listOf(
                TrivyTaskNames.SECRETS,
                TrivyTaskNames.LICENSES,
                TrivyTaskNames.VULNERABILITIES,
                TrivyTaskNames.MISCONFIGURATION,
                TrivyTaskNames.FILESYSTEM
            )
            registration shouldNotBe null
            val cache = registration?.service?.get()
            scans.forEach { scan -> servicesOf(scan) shouldContain cache }
        }
    }
})
