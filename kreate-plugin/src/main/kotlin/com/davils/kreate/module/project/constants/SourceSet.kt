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

package com.davils.kreate.module.project.constants

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Adds the generated build constants path to the project's Kotlin source sets.
 *
 * This function handles both Kotlin Multiplatform (adding to `commonMain`)
 * and Kotlin JVM (adding to `main`) projects.
 *
 * @param path The absolute path to the generated source directory.
 * @since 1.0.0
 */
internal fun Project.addBuildConstantsToSourceSets(path: String) {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain") {
                kotlin.srcDir(path)
            }
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        configure<KotlinJvmExtension> {
            sourceSets.getByName("main") {
                kotlin.srcDir(path)
            }
        }
    }
}
