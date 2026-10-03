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
import com.davils.kreate.gradle.configurePom
import com.davils.kreate.publish.PublishExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPomInternal
import org.gradle.testfixtures.ProjectBuilder

class PomConfigurationTest : FunSpec({
    fun project(): Project {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply(KreatePlugin::class.java)
        return project
    }

    fun publishOf(project: Project): PublishExtension {
        val kreate = project.extensions.getByType(KreateExtension::class.java)
        return kreate.project.publish
    }

    fun pomOf(project: Project): MavenPomInternal {
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        val publication = publishing.publications.create("maven", MavenPublication::class.java)
        return publication.pom as MavenPomInternal
    }

    context("POM configuration") {
        test("copies the project's name, description and publication details") {
            val project = project()
            val publish = publishOf(project)
            publish.inceptionYear.set(2021)
            publish.website.set("https://kreate.example")
            val pom = pomOf(project)

            pom.configurePom(publish, "Kreate", "Build conventions")

            pom.name.get() shouldBe "Kreate"
            pom.description.get() shouldBe "Build conventions"
            pom.inceptionYear.get() shouldBe "2021"
            pom.url.get() shouldBe "https://kreate.example"
        }

        test("copies every declared issue tracker, CI and SCM value") {
            val project = project()
            val publish = publishOf(project)
            publish.pom {
                issueManagement {
                    system.set("YouTrack")
                    url.set("https://youtrack.example")
                }
                ciManagement {
                    system.set("GitLab CI")
                    url.set("https://gitlab.example/pipelines")
                }
                scm {
                    url.set("https://gitlab.example/kreate")
                    connection.set("scm:git:https://gitlab.example/kreate.git")
                    developerConnection.set("scm:git:ssh://gitlab.example/kreate.git")
                }
            }
            val pom = pomOf(project)

            pom.configurePom(publish, null, null)

            val issues = pom.issueManagement.shouldNotBeNull()
            val ci = pom.ciManagement.shouldNotBeNull()
            val scm = pom.scm.shouldNotBeNull()
            issues.system.get() shouldBe "YouTrack"
            issues.url.get() shouldBe "https://youtrack.example"
            ci.system.get() shouldBe "GitLab CI"
            ci.url.get() shouldBe "https://gitlab.example/pipelines"
            scm.url.get() shouldBe "https://gitlab.example/kreate"
            scm.connection.get() shouldBe "scm:git:https://gitlab.example/kreate.git"
            scm.developerConnection.get() shouldBe "scm:git:ssh://gitlab.example/kreate.git"
        }

        test("copies every declared license and developer value") {
            val project = project()
            val publish = publishOf(project)
            publish.pom {
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("davils")
                        name.set("Davils")
                        email.set("dev@davils.example")
                        organization.set("Davils")
                        timezone.set("Europe/Berlin")
                    }
                }
            }
            val pom = pomOf(project)

            pom.configurePom(publish, null, null)

            val license = pom.licenses.single()
            license.name.get() shouldBe "Apache-2.0"
            license.url.get() shouldBe "https://www.apache.org/licenses/LICENSE-2.0"
            license.distribution.get() shouldBe "repo"
            val developer = pom.developers.single()
            developer.id.get() shouldBe "davils"
            developer.name.get() shouldBe "Davils"
            developer.email.get() shouldBe "dev@davils.example"
            developer.organization.get() shouldBe "Davils"
            developer.timezone.get() shouldBe "Europe/Berlin"
        }

        test("leaves everything undeclared unset") {
            val project = project()
            val publish = publishOf(project)
            val pom = pomOf(project)

            pom.configurePom(publish, null, null)

            pom.name.isPresent shouldBe false
            pom.description.isPresent shouldBe false
            pom.url.isPresent shouldBe false
            pom.issueManagement.shouldNotBeNull().system.isPresent shouldBe false
            pom.ciManagement.shouldNotBeNull().url.isPresent shouldBe false
            pom.scm.shouldNotBeNull().url.isPresent shouldBe false
            pom.licenses.single().name.isPresent shouldBe false
            pom.developers.single().id.isPresent shouldBe false
        }
    }
})
