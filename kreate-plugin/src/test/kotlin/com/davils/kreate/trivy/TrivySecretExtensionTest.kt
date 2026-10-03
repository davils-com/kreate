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

package com.davils.kreate.trivy

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreatePlugin
import com.davils.kreate.freshDirectory
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

class TrivySecretExtensionTest : FunSpec({
    val workspace = tempdir()

    fun gradleHomeOutsideTheProject(): File = workspace.freshDirectory("gradle-home")

    fun project(projectDirectory: File): Project {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(projectDirectory)
        builder.withGradleUserHomeDir(gradleHomeOutsideTheProject())
        builder.withName("sample")
        val project = builder.build()
        project.pluginManager.apply(KreatePlugin::class.java)
        return project
    }

    fun Project.secrets() =
        extensions.getByType(KreateExtension::class.java).trivy.secrets

    fun write(projectDirectory: File, path: String): File {
        val file = File(projectDirectory, path)
        file.parentFile.mkdirs()
        file.writeText("token: not-a-real-secret")
        return file
    }

    fun scannedNames(project: Project): List<String> {
        val files = project.secrets().sourceFiles.files
        return files.map { it.name }.sorted()
    }

    context("TrivySecretExtension") {
        context("running on check") {
            test("is on by default") {
                val secrets = project(workspace.freshDirectory("project")).secrets()

                secrets.runOnCheck.get() shouldBe true
            }

            test("can be turned off") {
                val secrets = project(workspace.freshDirectory("project")).secrets()

                secrets.runOnCheck.set(false)

                secrets.runOnCheck.get() shouldBe false
            }
        }

        context("the default scope") {
            test("covers sources under src and configuration files anywhere") {
                val projectDirectory = workspace.freshDirectory("project")
                write(projectDirectory, "src/main/kotlin/Main.kt")
                write(projectDirectory, "gradle/libs.versions.toml")
                write(projectDirectory, "application.yaml")
                write(projectDirectory, "nested/module/config.json")
                write(projectDirectory, "gradle.properties")

                val scanned = scannedNames(project(projectDirectory))

                scanned shouldContainExactly listOf(
                    "Main.kt",
                    "application.yaml",
                    "config.json",
                    "gradle.properties"
                )
            }

            test("leaves generated output out, so no task's output is an input of the scan") {
                val projectDirectory = workspace.freshDirectory("project")
                write(projectDirectory, "application.yaml")
                write(projectDirectory, "build/resources/main/application.yaml")
                write(projectDirectory, "build/kotlin/compileKotlinJvm/cacheable/last-build.json")
                write(projectDirectory, ".gradle/configuration-cache/entry.json")
                write(projectDirectory, ".kotlin/sessions/session.properties")

                val scanned = project(projectDirectory).secrets().sourceFiles.files

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

        context("narrowing the scope") {
            test("setFrom replaces the default") {
                val projectDirectory = workspace.freshDirectory("project")
                write(projectDirectory, "application.yaml")
                val only = write(projectDirectory, "src/main/resources/secrets.properties")

                val project = project(projectDirectory)
                project.secrets().sourceFiles.setFrom(project.files(only))

                scannedNames(project) shouldContainExactly listOf("secrets.properties")
            }

            test("from adds to the default rather than replacing it") {
                val projectDirectory = workspace.freshDirectory("project")
                write(projectDirectory, "application.yaml")
                val extra = write(projectDirectory, "deployment/values.txt")

                val project = project(projectDirectory)
                project.secrets().sourceFiles.from(project.files(extra))

                scannedNames(project) shouldContainExactly listOf("application.yaml", "values.txt")
            }
        }
    }
})
