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

package com.davils.kreate.publish.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreatePlugin
import com.davils.kreate.publish.MavenRepositoriesExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.gradle.testfixtures.ProjectBuilder

class PublicationSigningTest : FunSpec({
    fun repositories(): MavenRepositoriesExtension {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(KreatePlugin::class.java)
        val kreate = project.extensions.getByType(KreateExtension::class.java)
        return kreate.project.publish.repositories
    }

    context("signsPublications") {
        test("nothing is signed while no target is enabled") {
            signsPublications(repositories()) shouldBe false
        }

        test("Maven Central signs by default") {
            val repositories = repositories()
            repositories.mavenCentral { }

            signsPublications(repositories) shouldBe true
        }

        test("Maven Central does not sign once it is told not to") {
            val repositories = repositories()
            repositories.mavenCentral { signPublications.set(false) }

            signsPublications(repositories) shouldBe false
        }

        test("the Maven target does not sign by default") {
            val repositories = repositories()
            repositories.maven { }

            signsPublications(repositories) shouldBe false
        }

        test("the Maven target signs once it is told to") {
            val repositories = repositories()
            repositories.maven { signPublications.set(true) }

            signsPublications(repositories) shouldBe true
            signsMavenTarget(repositories) shouldBe true
        }

        test("a signing flag on a disabled Maven target is ignored") {
            val repositories = repositories()
            repositories.maven.signPublications.set(true)

            signsMavenTarget(repositories) shouldBe false
        }
    }
})
