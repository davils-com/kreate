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

import com.davils.kreate.module.getProjectVersion
import com.davils.kreate.module.platform.resolveFeatureProjectName
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for the Kreate extension defaults and the naming and versioning helpers.
 *
 * Defaults are part of the published contract: changing one silently changes the behaviour
 * of every consumer that did not set the value explicitly.
 */
@DisplayName("KreateExtension")
class KreateExtensionTest {

    private fun project(name: String = "sample"): Project =
        ProjectBuilder.builder().withName(name).build().also {
            it.pluginManager.apply(Kreate::class.java)
        }

    private fun extension(project: Project): KreateExtension =
        project.extensions.getByType(KreateExtension::class.java)

    @Nested
    @DisplayName("feature defaults")
    inner class FeatureDefaults {

        @Test
        @DisplayName("every optional feature is off by default")
        fun featuresAreOptIn() {
            val kreate = extension(project())

            kreate.platform.jvm.jni.enabled.get() shouldBe false
            kreate.platform.multiplatform.cInterop.enabled.get() shouldBe false
            kreate.trivy.enabled.get() shouldBe false
        }

        @Test
        @DisplayName("the JNI build type defaults to Release")
        fun jniBuildType() {
            extension(project()).platform.jvm.jni.buildType.get() shouldBe "Release"
        }

        @Test
        @DisplayName("JNI header generation is on once the feature is enabled")
        fun headerGenerationDefaultsOn() {
            extension(project()).platform.jvm.jni.headers.enabled.get() shouldBe true
        }

        @Test
        @DisplayName("JNI packaging is off so that upgrading does not change a JAR's contents")
        fun packagingDefaultsOff() {
            extension(project()).platform.jvm.jni.packaging.enabled.get() shouldBe false
        }

        @Test
        @DisplayName("packaged natives land under native/, where a Davils loader looks")
        fun packagingResourcePath() {
            extension(project()).platform.jvm.jni.packaging.resourcePath.get() shouldBe "native"
        }

        @Test
        @DisplayName("the generated loader is off, so the weakest loader is not the default one")
        fun loaderDefaultsOff() {
            extension(project()).platform.jvm.jni.packaging.generateLoader.get() shouldBe false
        }

        @Test
        @DisplayName("a digest manifest is written, because nobody should hash a release by hand")
        fun digestManifestDefaultsOn() {
            extension(project()).platform.jvm.jni.packaging.digestManifest.get() shouldBe true
        }

        @Test
        @DisplayName("does not inject repositories or a compiler plugin into the consumer")
        fun intrusiveDefaultsAreOptIn() {
            // Until 2.0.0 both were unconditional. Injecting repositories breaks builds that
            // resolve through an internal mirror, and applying the serialization compiler
            // plugin charged every project for a feature most never used.
            val kreate = extension(project())

            kreate.project.applyDefaultRepositories.get() shouldBe false
            kreate.project.applySerializationPlugin.get() shouldBe false
        }

        @Test
        @DisplayName("no CMake executable or generator is pinned by default")
        fun toolchainDefaultsAreUnset() {
            val jni = extension(project()).platform.jvm.jni

            jni.cmakeExecutable.isPresent shouldBe false
            jni.generator.isPresent shouldBe false
        }
    }

    @Nested
    @DisplayName("feature project naming")
    inner class FeatureNaming {

        @Test
        @DisplayName("falls back to the Gradle project name")
        fun fallsBackToProjectName() {
            val gradleProject = project("sample")
            val kreate = extension(gradleProject)

            gradleProject.resolveFeatureProjectName(kreate, kreate.platform.jvm.jni.nameOverride) shouldBe "sample"
        }

        @Test
        @DisplayName("prefers the Kreate project name over the Gradle one")
        fun prefersKreateName() {
            val gradleProject = project("sample")
            val kreate = extension(gradleProject)
            kreate.project.name.set("Configured")

            gradleProject.resolveFeatureProjectName(kreate, kreate.platform.jvm.jni.nameOverride) shouldBe "configured"
        }

        @Test
        @DisplayName("prefers an explicit feature override over everything else")
        fun prefersOverride() {
            val gradleProject = project("sample")
            val kreate = extension(gradleProject)
            kreate.project.name.set("Configured")
            kreate.platform.jvm.jni.nameOverride.set("mylib")

            gradleProject.resolveFeatureProjectName(kreate, kreate.platform.jvm.jni.nameOverride) shouldBe "mylib"
        }

        @Test
        @DisplayName("sanitizes names so they are valid CMake targets and JNI symbols")
        fun sanitizesName() {
            val gradleProject = project("My-Sample")
            val kreate = extension(gradleProject)

            gradleProject.resolveFeatureProjectName(kreate, kreate.platform.jvm.jni.nameOverride) shouldBe "my_sample"
        }
    }

    @Nested
    @DisplayName("version resolution")
    inner class VersionResolution {

        @Test
        @DisplayName("uses the project property when no CI tag is set")
        fun usesProjectProperty() {
            val gradleProject = ProjectBuilder.builder().build()
            gradleProject.extensions.extraProperties.set("customVersion", "3.1.4")

            gradleProject.getProjectVersion("KREATE_NO_SUCH_ENV", "customVersion") shouldBe "3.1.4"
        }

        @Test
        @DisplayName("falls back to 1.0.0 when neither source provides a version")
        fun fallsBackToDefault() {
            val gradleProject = ProjectBuilder.builder().build()

            gradleProject.getProjectVersion("KREATE_NO_SUCH_ENV", "alsoMissing") shouldBe "1.0.0"
        }

        @Test
        @DisplayName("ignores Gradle's 'unspecified' placeholder")
        fun ignoresUnspecified() {
            val gradleProject = ProjectBuilder.builder().build()
            gradleProject.extensions.extraProperties.set("placeholder", "unspecified")

            gradleProject.getProjectVersion("KREATE_NO_SUCH_ENV", "placeholder") shouldBe "1.0.0"
        }
    }
}
