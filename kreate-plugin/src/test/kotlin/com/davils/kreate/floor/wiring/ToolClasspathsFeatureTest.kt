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

package com.davils.kreate.floor.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.evaluatedKreateProject
import com.davils.kreate.floor.version.VersionFloorRule
import com.davils.kreate.freshDirectory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.ModuleVersionSelector

private fun detailsOf(group: String, name: String, version: String): DependencyResolveDetails {
    val selector = mockk<ModuleVersionSelector>()
    every { selector.group } returns group
    every { selector.name } returns name
    every { selector.version } returns version
    val details = mockk<DependencyResolveDetails>(relaxed = true)
    every { details.requested } returns selector
    return details
}

class ToolClasspathsFeatureTest : FunSpec({
    val workspace = tempdir()

    context("a floor") {
        test("raises a module below it and says why") {
            val details = detailsOf("org.bouncycastle", "bcprov-jdk18on", "1.80")
            val action = VersionFloorAction(listOf(VersionFloorRule("org.bouncycastle", "1.85", emptySet())))

            action.execute(details)

            verify { details.useVersion("1.85") }
            verify { details.because("Raised to 1.85, the version floor of the tool classpaths") }
        }

        test("leaves a module at or above it alone") {
            val details = detailsOf("org.bouncycastle", "bcprov-jdk18on", "1.85")
            val action = VersionFloorAction(listOf(VersionFloorRule("org.bouncycastle", "1.85", emptySet())))

            action.execute(details)

            verify(exactly = 0) { details.useVersion(any()) }
        }

        test("records the excluded modules") {
            val project = evaluatedKreateProject(workspace.freshDirectory(), "org.jetbrains.kotlin.jvm") { kreate ->
                kreate.project.toolClasspaths {
                    floor("com.fasterxml.jackson", "2.22.3") { exclude("jackson-annotations") }
                    floor("org.bouncycastle", "1.85")
                }
            }
            val kreate = project.extensions.getByType(KreateExtension::class.java)
            val floors = kreate.project.toolClasspaths.declaredFloors()

            floors.map { floor -> floor.group to floor.version } shouldBe listOf(
                "com.fasterxml.jackson" to "2.22.3",
                "org.bouncycastle" to "1.85"
            )
            floors.first().excluded shouldBe setOf("jackson-annotations")
        }

        test("needs a group and a numeric version") {
            evaluatedKreateProject(workspace.freshDirectory(), "org.jetbrains.kotlin.jvm") { kreate ->
                val classpaths = kreate.project.toolClasspaths
                shouldThrow<IllegalArgumentException> { classpaths.floor(" ", "1.0") }
                shouldThrow<IllegalArgumentException> { classpaths.floor("org.example", "latest.release") }
            }
        }
    }
})
