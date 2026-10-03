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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class VersionResolutionTest : FunSpec({
    val unsetVariable = "KREATE_NO_SUCH_ENV"

    fun project(): Project = ProjectBuilder.builder().build()

    context("version resolution") {
        test("uses the project property when no CI tag is set") {
            val gradleProject = project()
            gradleProject.extensions.extraProperties.set("customVersion", "3.1.4")

            gradleProject.getProjectVersion(unsetVariable, "customVersion") shouldBe "3.1.4"
        }

        test("falls back to 1.0.0 when neither source provides a version") {
            project().getProjectVersion(unsetVariable, "alsoMissing") shouldBe "1.0.0"
        }

        test("ignores Gradle's 'unspecified' placeholder") {
            val gradleProject = project()
            gradleProject.extensions.extraProperties.set("placeholder", Project.DEFAULT_VERSION)

            gradleProject.getProjectVersion(unsetVariable, "placeholder") shouldBe "1.0.0"
        }

        test("ignores a blank project property") {
            val gradleProject = project()
            gradleProject.extensions.extraProperties.set("blank", " ")

            gradleProject.getProjectVersion(unsetVariable, "blank") shouldBe "1.0.0"
        }
    }
})
