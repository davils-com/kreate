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

import com.davils.buildlogic.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    id("kreate.kotlin-conventions")
    id("kreate.quality-conventions")
    id("kreate.coverage-conventions")
    id("kreate.publish-conventions")
}

dependencies {
    implementation(gradleApi())
    implementation(libs.bundles.kreate.plugin)

    implementation("${Project.Identity.GROUP}:kreate-settings:$version")

    testImplementation(platform(libs.junit.bom))
    testImplementation(gradleTestKit())
    testImplementation(libs.bundles.kreate.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    compilerOptions {
        optIn.add("com.davils.kreate.settings.InternalKreateApi")
    }
}

val functionalTest: SourceSet = sourceSets.create("functionalTest")

val coverageAgent: Configuration = configurations.create("coverageAgent") {
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    coverageAgent(variantOf(libs.jacoco.agent) { classifier("runtime") })
}

val functionalCoverageFile: Provider<RegularFile> = layout.buildDirectory.file("kover/functional/functionalTest.exec")

configurations[functionalTest.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())
configurations[functionalTest.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.testRuntimeOnly.get())

gradlePlugin {
    vcsUrl = Project.VersionControl.SCM_URL
    website = Project.Organization.WEBSITE_URL

    testSourceSets(functionalTest)

    plugins {
        create(Project.Identity.NAME.lowercase()) {
            id = "${Project.Identity.GROUP}.${Project.Identity.NAME.lowercase()}"
            description = Project.Identity.DESCRIPTION
            displayName = Project.Identity.NAME
            implementationClass = "com.davils.kreate.KreatePlugin"
            tags = listOf(
                "kotlin",
                "multiplatform",
                "jni",
                "cinterop",
                "detekt",
                "trivy",
                "publishing",
                "conventions",
                "davils"
            )
        }
    }
}

val functionalTestTask = tasks.register<Test>("functionalTest") {
    description = "Runs the TestKit based functional tests against real Gradle builds."
    group = LifecycleBasePlugin.VERIFICATION_GROUP

    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath

    val skipsSlowTests = providers.gradleProperty("kreate.test.skipSlow").isPresent
    if (skipsSlowTests) {
        systemProperty("kotest.tags", "!Slow")
    }

    systemProperty("kreate.test.gradleVersion", gradle.gradleVersion)
    systemProperty("kreate.test.minGradleVersion", Project.Compatibility.MIN_GRADLE_VERSION)

    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)

    val agentFiles: FileCollection = coverageAgent
    val coverageFile = functionalCoverageFile
    inputs.files(agentFiles).withPropertyName("coverageAgent")
    outputs.file(coverageFile).withPropertyName("coverageFile")
    jvmArgumentProviders.add(
        CommandLineArgumentProvider {
            listOf(
                "-Dkreate.test.coverageAgent=${agentFiles.singleFile.absolutePath}",
                "-Dkreate.test.coverageFile=${coverageFile.get().asFile.absolutePath}"
            )
        }
    )
    doFirst { coverageFile.get().asFile.delete() }
}

kover {
    currentProject {
        instrumentation {
            disabledForTestTasks.add(functionalTestTask.name)
        }
    }

    reports {
        total {
            additionalBinaryReports.add(functionalCoverageFile.map { file -> file.asFile })
        }
    }
}

val coverageReportSuffixes: List<String> = listOf("Report", "Log", "Verify")

tasks.matching { task -> isCoverageReport(task.name, coverageReportSuffixes) }.configureEach {
    dependsOn(functionalTestTask)
}

fun isCoverageReport(taskName: String, suffixes: List<String>): Boolean {
    val isKoverTask = taskName.startsWith("kover")
    return isKoverTask && suffixes.any { suffix -> taskName.endsWith(suffix) }
}

tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
    dependsOn(functionalTestTask)
}

detekt {
    source.from(functionalTest.allSource.srcDirs)
}

tasks.named<KotlinCompile>("compileFunctionalTestKotlin") {
    compilerOptions.freeCompilerArgs.add("-Xexplicit-api=disable")
}
