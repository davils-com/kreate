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

package com.davils.kreate

import com.davils.kreate.gradle.resolveFeatureProjectName
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class KreateExtensionTest : FunSpec({
    fun project(name: String = "sample"): Project {
        val builder = ProjectBuilder.builder()
        val project = builder.withName(name).build()
        project.pluginManager.apply(KreatePlugin::class.java)
        return project
    }

    fun extension(project: Project): KreateExtension =
        project.extensions.getByType(KreateExtension::class.java)

    fun featureProjectNameOf(gradleProject: Project, kreate: KreateExtension): String =
        gradleProject.resolveFeatureProjectName(kreate, kreate.platform.jvm.jni.nameOverride)

    context("KreateExtension") {
        context("feature defaults") {
            test("every optional feature is off by default") {
                val kreate = extension(project())

                kreate.platform.jvm.jni.enabled.get() shouldBe false
                kreate.platform.multiplatform.cInterop.enabled.get() shouldBe false
                kreate.trivy.enabled.get() shouldBe false
            }

            test("the JNI build type defaults to Release") {
                extension(project()).platform.jvm.jni.buildType.get() shouldBe "Release"
            }

            test("JNI header generation is on once the feature is enabled") {
                extension(project()).platform.jvm.jni.headers.enabled.get() shouldBe true
            }

            test("JNI packaging is off so that upgrading does not change a JAR's contents") {
                extension(project()).platform.jvm.jni.packaging.enabled.get() shouldBe false
            }

            test("packaged natives land under native/, where a Davils loader looks") {
                extension(project()).platform.jvm.jni.packaging.resourcePath.get() shouldBe "native"
            }

            test("the generated loader is off, so the weakest loader is not the default one") {
                extension(project()).platform.jvm.jni.packaging.generateLoader.get() shouldBe false
            }

            test("a digest manifest is written, because nobody should hash a release by hand") {
                extension(project()).platform.jvm.jni.packaging.digestManifest.get() shouldBe true
            }

            test("does not inject repositories or a compiler plugin into the consumer") {
                val kreate = extension(project())

                kreate.project.applyDefaultRepositories.get() shouldBe false
                kreate.project.applySerializationPlugin.get() shouldBe false
            }

            test("no CMake executable or generator is pinned by default") {
                val jni = extension(project()).platform.jvm.jni

                jni.cmakeExecutable.isPresent shouldBe false
                jni.generator.isPresent shouldBe false
            }
        }

        context("feature project naming") {
            test("falls back to the Gradle project name") {
                val gradleProject = project("sample")
                val kreate = extension(gradleProject)

                featureProjectNameOf(gradleProject, kreate) shouldBe "sample"
            }

            test("prefers the Kreate project name over the Gradle one") {
                val gradleProject = project("sample")
                val kreate = extension(gradleProject)
                kreate.project.name.set("Configured")

                featureProjectNameOf(gradleProject, kreate) shouldBe "configured"
            }

            test("prefers an explicit feature override over everything else") {
                val gradleProject = project("sample")
                val kreate = extension(gradleProject)
                kreate.project.name.set("Configured")
                kreate.platform.jvm.jni.nameOverride.set("mylib")

                featureProjectNameOf(gradleProject, kreate) shouldBe "mylib"
            }

            test("sanitizes names so they are valid CMake targets and JNI symbols") {
                val gradleProject = project("My-Sample")
                val kreate = extension(gradleProject)

                featureProjectNameOf(gradleProject, kreate) shouldBe "my_sample"
            }
        }
    }
})
