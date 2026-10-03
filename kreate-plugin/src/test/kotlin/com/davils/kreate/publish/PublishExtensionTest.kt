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

class PublishExtensionTest : FunSpec({
    fun publish(): PublishExtension {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(KreatePlugin::class.java)
        val kreate = project.extensions.getByType(KreateExtension::class.java)
        return kreate.project.publish
    }

    context("PublishExtension") {
        test("publishing is off until it is asked for") {
            publish().enabled.get() shouldBe false
        }

        test("the pom block configures the issue tracker and the CI system") {
            val publish = publish()
            publish.pom {
                issueManagement {
                    system.set("YouTrack")
                    url.set("https://youtrack.example")
                }
                ciManagement {
                    system.set("GitLab CI")
                    url.set("https://gitlab.example")
                }
            }

            publish.pom.issueManagement.system.get() shouldBe "YouTrack"
            publish.pom.ciManagement.url.get() shouldBe "https://gitlab.example"
        }

        test("the pom block configures the license, the developer and the SCM") {
            val publish = publish()
            publish.pom {
                licenses { license { name.set("Apache-2.0") } }
                developers { developer { id.set("davils") } }
                scm { url.set("https://git.example/kreate") }
            }
            val pom = publish.pom

            pom.licenses.license.name.get() shouldBe "Apache-2.0"
            pom.developers.developer.id.get() shouldBe "davils"
            pom.scm.url.get() shouldBe "https://git.example/kreate"
        }

        test("the repositories block configures Maven Central") {
            val publish = publish()
            publish.repositories { mavenCentral { enabled.set(true) } }

            publish.repositories.mavenCentral.enabled.get() shouldBe true
        }
    }
})
