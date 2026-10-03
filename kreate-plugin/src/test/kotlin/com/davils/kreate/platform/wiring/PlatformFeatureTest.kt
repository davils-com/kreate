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
import com.davils.kreate.platform.PlatformExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class PlatformFeatureTest : FunSpec({
    val workspace = tempdir()

    fun evaluated(pluginId: String, configure: (PlatformExtension) -> Unit): Project {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        project.pluginManager.apply(pluginId)
        project.pluginManager.apply(KreatePlugin::class.java)
        val kreate = project.extensions.getByType(KreateExtension::class.java)
        configure(kreate.platform)
        (project as ProjectInternal).evaluate()
        return project
    }

    fun javaOf(project: Project): JavaPluginExtension = project.extensions.getByType(JavaPluginExtension::class.java)

    context("platform defaults") {
        test("a JVM project compiles for Java 17 in explicit API mode") {
            val project = evaluated("org.jetbrains.kotlin.jvm") { }
            val kotlin = project.extensions.getByType(KotlinJvmProjectExtension::class.java)

            javaOf(project).targetCompatibility shouldBe JavaVersion.VERSION_17
            kotlin.explicitApi shouldBe ExplicitApiMode.Strict
            kotlin.compilerOptions.allWarningsAsErrors.get() shouldBe false
        }

        test("a multiplatform project compiles in explicit API mode") {
            val project = evaluated("org.jetbrains.kotlin.multiplatform") { }
            val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

            kotlin.explicitApi shouldBe ExplicitApiMode.Strict
        }
    }

    context("configured platform") {
        test("a JVM project takes the configured Java version and compiler strictness") {
            val project = evaluated("org.jetbrains.kotlin.jvm") { platform ->
                platform.javaVersion.set(JavaVersion.VERSION_21)
                platform.explicitApi.set(false)
                platform.allWarningsAsErrors.set(true)
            }
            val kotlin = project.extensions.getByType(KotlinJvmProjectExtension::class.java)

            javaOf(project).sourceCompatibility shouldBe JavaVersion.VERSION_21
            kotlin.explicitApi shouldBe null
            kotlin.compilerOptions.allWarningsAsErrors.get() shouldBe true
        }

        test("a multiplatform project takes the configured compiler strictness") {
            val project = evaluated("org.jetbrains.kotlin.multiplatform") { platform ->
                platform.explicitApi.set(false)
                platform.allWarningsAsErrors.set(true)
            }
            val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

            kotlin.explicitApi shouldBe null
            kotlin.compilerOptions.allWarningsAsErrors.get() shouldBe true
        }

        test("the multiplatform and JVM blocks configure their interop settings") {
            val project = evaluated("org.jetbrains.kotlin.jvm") { platform ->
                platform.multiplatform { cInterop { nameOverride.set("bridge") } }
                platform.jvm { jni { nameOverride.set("native") } }
            }
            val platform = project.extensions.getByType(KreateExtension::class.java).platform

            platform.multiplatform.cInterop.nameOverride.get() shouldBe "bridge"
            platform.jvm.jni.nameOverride.get() shouldBe "native"
        }
    }
})
