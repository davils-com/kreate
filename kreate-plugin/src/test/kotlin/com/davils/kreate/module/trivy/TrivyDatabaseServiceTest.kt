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

package com.davils.kreate.module.trivy

import com.davils.kreate.Kreate
import com.davils.kreate.KreateExtension
import com.davils.kreate.KreateTasks
import io.kotest.matchers.shouldBe
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests for the service that keeps vulnerability scans from running concurrently.
 *
 * Every Trivy process updates the one vulnerability database in the user's cache. Scans of
 * several modules running side by side - which the configuration cache does by default - replaced
 * it under each other and crashed Trivy with SIGSEGV and SIGBUS.
 */
@DisplayName("TrivyDatabaseService")
class TrivyDatabaseServiceTest {

    @TempDir
    lateinit var projectDirectory: File

    @TempDir
    lateinit var gradleHome: File

    @Test
    @DisplayName("admits one vulnerability scan at a time")
    fun admitsOneScanAtATime() {
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDirectory)
            .withGradleUserHomeDir(gradleHome)
            .build()
        project.pluginManager.apply(Kreate::class.java)
        project.extensions.getByType(KreateExtension::class.java).trivy.enabled.set(true)

        (project as ProjectInternal).evaluate()
        project.tasks.getByName(KreateTasks.Trivy.VULNERABILITIES)

        val registrations = project.gradle.sharedServices.registrations
        registrations.getByName(TRIVY_DATABASE_SERVICE).maxParallelUsages.get() shouldBe 1
    }
}
