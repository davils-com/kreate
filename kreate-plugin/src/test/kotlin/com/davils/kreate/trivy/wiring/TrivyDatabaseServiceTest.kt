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
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder

class TrivyDatabaseServiceTest : FunSpec({
    val workspace = tempdir()

    context("TrivyDatabaseService") {
        test("admits one vulnerability scan at a time") {
            val builder = ProjectBuilder.builder()
            builder.withProjectDir(workspace.freshDirectory("project"))
            builder.withGradleUserHomeDir(workspace.freshDirectory("gradle-home"))
            val project = builder.build()
            project.pluginManager.apply(KreatePlugin::class.java)
            project.extensions.getByType(KreateExtension::class.java).trivy.enabled.set(true)

            (project as ProjectInternal).evaluate()
            project.tasks.getByName(TrivyTaskNames.VULNERABILITIES)

            val registrations = project.gradle.sharedServices.registrations
            registrations.getByName(TRIVY_DATABASE_SERVICE).maxParallelUsages.get() shouldBe 1
        }
    }
})
