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

package com.davils.kreate.platform

import com.davils.kreate.freshDirectory
import com.davils.kreate.host.Architecture
import com.davils.kreate.host.HostPlatform
import com.davils.kreate.host.OperatingSystem
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

private const val LINUX: String = "Linux"

private fun expectedHostTarget(): String {
    val host = HostPlatform.current()
    val operatingSystem = host.requireOperatingSystem("the expected target")
    return when (operatingSystem) {
        OperatingSystem.WINDOWS -> "mingwX64"
        OperatingSystem.MACOS -> "macosArm64"
        OperatingSystem.LINUX -> linuxTarget(host)
    }
}

private fun linuxTarget(host: HostPlatform): String {
    val architecture = host.requireArchitecture("the expected target")
    if (architecture == Architecture.X64) return "linuxX64"
    return "linuxArm64"
}

class CurrentOsTest : FunSpec({
    val workspace = tempdir()

    fun multiplatform(): KotlinMultiplatformExtension {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        return project.extensions.getByType(KotlinMultiplatformExtension::class.java)
    }

    fun nativeTargetNames(kotlin: KotlinMultiplatformExtension): List<String> {
        val targets = kotlin.targets.withType(KotlinNativeTarget::class.java)
        return targets.map { target -> target.name }
    }

    context("currentOs") {
        test("declares the Kotlin/Native target of the machine the build runs on") {
            val kotlin = multiplatform()

            kotlin.currentOs()

            nativeTargetNames(kotlin) shouldContainExactly listOf(expectedHostTarget())
        }

        test("hands the declared target to the configuration block") {
            val kotlin = multiplatform()
            val configured = mutableListOf<String>()

            kotlin.currentOs { configured.add(name) }

            configured shouldContainExactly listOf(expectedHostTarget())
        }
    }

    context("declareHostTarget") {
        withData(
            nameFn = { (osName, archName, target) -> "$osName on $archName declares $target" },
            Triple("Windows 11", "amd64", "mingwX64"),
            Triple("Mac OS X", "aarch64", "macosArm64"),
            Triple(LINUX, "amd64", "linuxX64"),
            Triple(LINUX, "aarch64", "linuxArm64")
        ) { (osName, archName, target) ->
            val kotlin = multiplatform()

            kotlin.declareHostTarget(HostPlatform(osName, archName)) {}

            nativeTargetNames(kotlin) shouldContainExactly listOf(target)
        }

        test("adds no target on an operating system Kreate does not recognise") {
            val kotlin = multiplatform()

            kotlin.declareHostTarget(HostPlatform("Plan 9", "amd64")) {}

            nativeTargetNames(kotlin).shouldBeEmpty()
        }

        withData(
            nameFn = { archName -> "adds no target on a Linux host with the unsupported architecture $archName" },
            "riscv64",
            "ppc64le"
        ) { archName ->
            val kotlin = multiplatform()
            val configured = mutableListOf<String>()

            kotlin.declareHostTarget(HostPlatform(LINUX, archName)) { configured.add(name) }

            nativeTargetNames(kotlin).shouldBeEmpty()
            configured.shouldBeEmpty()
        }
    }
})
