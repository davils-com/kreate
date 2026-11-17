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

package com.davils.kreate.packaging.wiring

import com.davils.kreate.evaluatedKreateProject
import com.davils.kreate.freshDirectory
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.jvm.tasks.Jar

private const val JVM_PLUGIN: String = "org.jetbrains.kotlin.jvm"

private fun Project.jar(): Jar = tasks.getByName("jar") as Jar

private fun valueOf(attribute: Any?): Any? = (attribute as? Provider<*>)?.get() ?: attribute

class ArchivesWiringTest : FunSpec({
    val workspace = tempdir()

    context("the manifest of the main jar") {
        test("names nothing by default") {
            val project = evaluatedKreateProject(workspace.freshDirectory(), JVM_PLUGIN) { }

            project.jar().manifest.attributes shouldNotContainKey "Implementation-Title"
        }

        test("names the module, the version and the vendor") {
            val project = evaluatedKreateProject(workspace.freshDirectory(), JVM_PLUGIN) { kreate ->
                kreate.platform.jvm { moduleName.set("com.example.library") }
                kreate.project.archives {
                    implementationManifest.set(true)
                    vendor.set("Example")
                }
            }
            project.version = "1.2.0"
            val attributes = project.jar().manifest.attributes

            attributes["Implementation-Title"] shouldBe "com.example.library"
            valueOf(attributes["Implementation-Version"]) shouldBe "1.2.0"
            attributes["Implementation-Vendor"] shouldBe "Example"
        }

        test("falls back to the project name and leaves out an unknown vendor") {
            val directory = workspace.freshDirectory("library")
            val project = evaluatedKreateProject(directory, JVM_PLUGIN) { kreate ->
                kreate.project.archives { implementationManifest.set(true) }
            }
            val attributes = project.jar().manifest.attributes

            attributes["Implementation-Title"] shouldBe project.name
            attributes shouldNotContainKey "Implementation-Vendor"
        }
    }
})
