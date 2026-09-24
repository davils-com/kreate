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
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests for the scope of the secret scan.
 *
 * What this collection resolves to is the whole contract of the scan: a file missing from it is a
 * credential nobody is looking for, and a file in it that another task wrote is a build Gradle
 * refuses to run at all.
 */
@DisplayName("TrivySecretExtension")
class TrivySecretExtensionTest {

    @TempDir
    lateinit var projectDirectory: File

    /**
     * Gradle's own scratch directory, deliberately outside the project.
     *
     * ProjectBuilder puts it under the project directory by default, and the journal it writes there
     * is a `.properties` file the default scope then matches - a file the harness created, in a
     * directory no real build has.
     */
    @TempDir
    lateinit var gradleHome: File

    private fun project(): Project =
        ProjectBuilder.builder()
            .withProjectDir(projectDirectory)
            .withGradleUserHomeDir(gradleHome)
            .withName("sample")
            .build()
            .also { it.pluginManager.apply(Kreate::class.java) }

    private fun Project.secrets() =
        extensions.getByType(KreateExtension::class.java).trivy.secrets

    private fun write(path: String): File {
        val file = File(projectDirectory, path)
        file.parentFile.mkdirs()
        file.writeText("token: not-a-real-secret")
        return file
    }

    private fun scannedNames(project: Project): List<String> =
        project.secrets().sourceFiles.files.map { it.name }.sorted()

    @Nested
    @DisplayName("running on check")
    inner class RunOnCheck {

        @Test
        @DisplayName("is on by default")
        fun onByDefault() {
            project().secrets().runOnCheck.get() shouldBe true
        }

        @Test
        @DisplayName("can be turned off")
        fun canBeTurnedOff() {
            val secrets = project().secrets()

            secrets.runOnCheck.set(false)

            secrets.runOnCheck.get() shouldBe false
        }
    }

    @Nested
    @DisplayName("the default scope")
    inner class DefaultScope {

        @Test
        @DisplayName("covers sources under src and configuration files anywhere")
        fun coversSourcesAndConfiguration() {
            write("src/main/kotlin/Main.kt")
            write("gradle/libs.versions.toml")
            write("application.yaml")
            write("nested/module/config.json")
            write("gradle.properties")

            val scanned = scannedNames(project())

            scanned shouldContainExactly listOf(
                "Main.kt",
                "application.yaml",
                "config.json",
                "gradle.properties"
            )
        }

        @Test
        @DisplayName("leaves generated output out, so no task's output is an input of the scan")
        fun excludesGeneratedOutput() {
            write("application.yaml")
            write("build/resources/main/application.yaml")
            write("build/kotlin/compileKotlinJvm/cacheable/last-build.json")
            write(".gradle/configuration-cache/entry.json")
            write(".kotlin/sessions/session.properties")

            val scanned = project().secrets().sourceFiles.files

            scanned.map { it.name } shouldContain "application.yaml"
            scanned shouldNotContain File(projectDirectory, "build/resources/main/application.yaml")
            scanned shouldNotContain File(
                projectDirectory,
                "build/kotlin/compileKotlinJvm/cacheable/last-build.json"
            )
            scanned shouldNotContain File(projectDirectory, ".gradle/configuration-cache/entry.json")
            scanned shouldNotContain File(projectDirectory, ".kotlin/sessions/session.properties")
        }
    }

    @Nested
    @DisplayName("narrowing the scope")
    inner class Narrowing {

        @Test
        @DisplayName("setFrom replaces the default")
        fun setFromReplaces() {
            write("application.yaml")
            val only = write("src/main/resources/secrets.properties")

            val project = project()
            project.secrets().sourceFiles.setFrom(project.files(only))

            scannedNames(project) shouldContainExactly listOf("secrets.properties")
        }

        @Test
        @DisplayName("from adds to the default rather than replacing it")
        fun fromAdds() {
            write("application.yaml")
            val extra = write("deployment/values.txt")

            val project = project()
            project.secrets().sourceFiles.from(project.files(extra))

            scannedNames(project) shouldContainExactly listOf("application.yaml", "values.txt")
        }
    }
}
