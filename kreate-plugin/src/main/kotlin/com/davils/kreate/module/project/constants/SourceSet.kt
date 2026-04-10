package com.davils.kreate.module.project.constants

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

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