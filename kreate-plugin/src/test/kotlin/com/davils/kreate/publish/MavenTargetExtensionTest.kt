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

package com.davils.kreate.publish

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreatePlugin
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.gradle.testfixtures.ProjectBuilder

class MavenTargetExtensionTest : FunSpec({
    fun repositories(): MavenRepositoriesExtension {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(KreatePlugin::class.java)
        val kreate = project.extensions.getByType(KreateExtension::class.java)
        return kreate.project.publish.repositories
    }

    context("MavenTargetExtension") {
        test("the target is off until it is asked for") {
            repositories().maven.enabled.get() shouldBe false
        }

        test("the block enables the target") {
            val repositories = repositories()
            repositories.maven { releases.set("https://repo.example.com/releases") }

            repositories.maven.enabled.get() shouldBe true
            repositories.maven.releases.get() shouldBe "https://repo.example.com/releases"
        }

        test("neither URL has a default") {
            val maven = repositories().maven

            maven.releases.isPresent shouldBe false
            maven.snapshots.isPresent shouldBe false
        }

        test("the repository and the credential variables carry neutral defaults") {
            val maven = repositories().maven

            maven.name.get() shouldBe "Maven"
            maven.usernameEnv.get() shouldBe "MAVEN_USERNAME"
            maven.passwordEnv.get() shouldBe "MAVEN_PASSWORD"
        }

        test("signing and plain http are off by default") {
            val maven = repositories().maven

            maven.signPublications.get() shouldBe false
            maven.allowInsecureProtocol.get() shouldBe false
        }
    }
})
