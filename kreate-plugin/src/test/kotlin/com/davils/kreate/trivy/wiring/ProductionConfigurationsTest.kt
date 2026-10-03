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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

private fun projectWith(pluginId: String?): Project {
    val project = ProjectBuilder.builder().build()
    pluginId?.let { id -> project.pluginManager.apply(id) }
    return project
}

class ProductionConfigurationsTest : FunSpec({
    context("productionConfigurations") {
        test("names the main classpaths of a Kotlin/JVM project") {
            val project = projectWith("org.jetbrains.kotlin.jvm")

            project.productionConfigurations() shouldContainExactlyInAnyOrder
                setOf("compileClasspath", "runtimeClasspath")
        }

        test("names the main classpaths of every multiplatform target") {
            val project = projectWith("org.jetbrains.kotlin.multiplatform")
            project.extensions.configure<KotlinMultiplatformExtension> { jvm() }

            val configurations = project.productionConfigurations()

            configurations shouldContainExactlyInAnyOrder
                setOf("jvmCompileClasspath", "jvmRuntimeClasspath", "metadataCompileClasspath")
        }

        test("falls back to the Java classpaths without a Kotlin plugin") {
            val project = projectWith(null)

            project.productionConfigurations() shouldContainExactlyInAnyOrder
                setOf("compileClasspath", "runtimeClasspath")
        }
    }
})
