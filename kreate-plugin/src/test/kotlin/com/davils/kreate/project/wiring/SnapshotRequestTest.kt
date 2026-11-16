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

package com.davils.kreate.project.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreatePlugin
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class SnapshotRequestTest : FunSpec({
    fun project(): Project {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(KreatePlugin::class.java)
        return project
    }

    fun kreate(project: Project): KreateExtension = project.extensions.getByType(KreateExtension::class.java)

    context("requestsSnapshot") {
        test("a build is a release until a snapshot is asked for") {
            val gradleProject = project()

            gradleProject.requestsSnapshot(kreate(gradleProject).project.version) shouldBe false
        }

        test("the DSL asks for a snapshot") {
            val gradleProject = project()
            val version = kreate(gradleProject).project.version
            version.snapshot.set(true)

            gradleProject.requestsSnapshot(version) shouldBe true
        }

        test("the DSL can rule a snapshot out") {
            val gradleProject = project()
            val version = kreate(gradleProject).project.version
            version.snapshot.set(false)

            gradleProject.requestsSnapshot(version) shouldBe false
        }
    }

    context("configureVersion") {
        test("a snapshot appends the suffix to the resolved version") {
            val gradleProject = project()
            gradleProject.extensions.extraProperties.set("declared", "2.0.0")

            gradleProject.configureVersion("KREATE_NO_SUCH_ENV", "declared", snapshot = true)

            gradleProject.version.toString() shouldBe "2.0.0-SNAPSHOT"
        }

        test("a snapshot never doubles a suffix that is already there") {
            val gradleProject = project()
            gradleProject.extensions.extraProperties.set("declared", "2.0.0-SNAPSHOT")

            gradleProject.configureVersion("KREATE_NO_SUCH_ENV", "declared", snapshot = true)

            gradleProject.version.toString() shouldBe "2.0.0-SNAPSHOT"
        }

        test("a release keeps the resolved version") {
            val gradleProject = project()
            gradleProject.extensions.extraProperties.set("declared", "2.0.0")

            gradleProject.configureVersion("KREATE_NO_SUCH_ENV", "declared", snapshot = false)

            gradleProject.version.toString() shouldBe "2.0.0"
        }
    }
})
