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

import org.gradle.language.base.plugins.LifecycleBasePlugin

plugins {
    `kotlin-dsl`
    id("kreate.kotlin-conventions")
    id("kreate.quality-conventions")
    id("kreate.coverage-conventions")
    id("kreate.publish-conventions")
}

dependencies {
    implementation(gradleApi())

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.kreate.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    compilerOptions {
        optIn.add("com.davils.kreate.settings.InternalKreateApi")
    }
}

gradlePlugin {
    vcsUrl = com.davils.buildlogic.Project.VersionControl.SCM_URL
    website = com.davils.buildlogic.Project.Organization.WEBSITE_URL

    plugins {
        create("kreateSettings") {
            id = "com.davils.kreate.settings"
            description = "Resolves locally published Davils artifacts, for local development."
            displayName = "Kreate settings"
            implementationClass = "com.davils.kreate.settings.KreateSettingsPlugin"
            tags = listOf(
                "kotlin",
                "local-development",
                "dependency-substitution",
                "conventions",
                "davils"
            )
        }
    }
}

val runtimeDependencies = configurations.runtimeClasspath.flatMap { classpath ->
    classpath.incoming.resolutionResult.rootComponent.map { root ->
        root.dependencies.map { it.requested.displayName }
    }
}

val verifyRuntimeClasspath = tasks.register("verifyRuntimeClasspath") {
    description = "Fails when the settings plugin would load anything but the Gradle API."
    group = LifecycleBasePlugin.VERIFICATION_GROUP

    val declared = runtimeDependencies
    inputs.property("runtimeDependencies", declared)
    doLast {
        val found = declared.get()
        check(found.isEmpty()) {
            "kreate-settings must not have runtime dependencies - each one would be loaded into the " +
                "settings class loader of every consuming build and shadow its own version. Found: " +
                found.joinToString()
        }
    }
}

tasks.named("check") {
    dependsOn(verifyRuntimeClasspath)
}
