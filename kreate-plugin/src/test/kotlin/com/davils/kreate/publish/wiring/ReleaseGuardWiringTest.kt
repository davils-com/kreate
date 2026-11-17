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

package com.davils.kreate.publish.wiring

import com.davils.kreate.evaluatedKreateProject
import com.davils.kreate.freshDirectory
import com.davils.kreate.publish.ReleaseGuardTaskNames
import com.davils.kreate.publish.task.ReleaseGuardCheck
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

private const val JVM_PLUGIN: String = "org.jetbrains.kotlin.jvm"

class ReleaseGuardWiringTest : FunSpec({
    val workspace = tempdir()

    fun publishing(enableGuard: Boolean): Project {
        val directory = workspace.freshDirectory()
        val repositoryDirectory = directory.resolve("repository")
        val repository = repositoryDirectory.toURI().toString()
        return evaluatedKreateProject(directory, JVM_PLUGIN) { kreate ->
            kreate.project.publish {
                enabled.set(true)
                repositories {
                    maven {
                        enabled.set(true)
                        releases.set(repository)
                        snapshots.set(repository)
                    }
                }
                releaseGuard {
                    enabled.set(enableGuard)
                    releaseBranch.set("trunk")
                    tagPrefix.set("v")
                }
            }
        }
    }

    context("the release guard") {
        test("is not registered by default") {
            val project = publishing(enableGuard = false)

            project.tasks.findByName(ReleaseGuardTaskNames.GUARD).shouldBeNull()
        }

        test("guards every publish task and knows the tools of the build") {
            val project = publishing(enableGuard = true)
            val guard = project.tasks.getByName(ReleaseGuardTaskNames.GUARD) as ReleaseGuardCheck
            val publishTasks = project.tasks.withType(PublishToMavenRepository::class.java)

            publishTasks.forEach { task ->
                task.taskDependencies.getDependencies(task) shouldContain guard
            }
            guard.releaseBranch.get() shouldBe "trunk"
            guard.tagPrefix.get() shouldBe "v"
            guard.tools.get() shouldContainKey "Kreate"
            guard.tools.get() shouldContainKey "Kotlin Gradle plugin"
        }
    }
})
