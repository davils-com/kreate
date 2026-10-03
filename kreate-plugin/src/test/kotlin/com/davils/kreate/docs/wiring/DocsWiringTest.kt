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

package com.davils.kreate.docs.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreatePlugin
import com.davils.kreate.docs.DocsExtension
import com.davils.kreate.freshDirectory
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.plugins.DokkaHtmlPluginParameters

private const val DOKKA_PLUGIN_ID = "org.jetbrains.dokka"

class DocsWiringTest : FunSpec({
    val workspace = tempdir()

    fun evaluated(configure: (DocsExtension) -> Unit): Project {
        val builder = ProjectBuilder.builder()
        builder.withName("library")
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        project.pluginManager.apply(KreatePlugin::class.java)
        val kreate = project.extensions.getByType(KreateExtension::class.java)
        configure(kreate.project.docs)
        (project as ProjectInternal).evaluate()
        return project
    }

    fun dokkaOf(project: Project): DokkaExtension = project.extensions.getByType(DokkaExtension::class.java)

    fun footerOf(dokka: DokkaExtension): DokkaHtmlPluginParameters {
        val html = dokka.pluginsConfiguration.getByName("html")
        return html as DokkaHtmlPluginParameters
    }

    context("documentation wiring") {
        test("applies no Dokka while documentation is disabled") {
            val project = evaluated { }

            project.pluginManager.hasPlugin(DOKKA_PLUGIN_ID) shouldBe false
        }

        test("applies Dokka once documentation is enabled") {
            val project = evaluated { docs -> docs.enabled.set(true) }

            project.pluginManager.hasPlugin(DOKKA_PLUGIN_ID) shouldBe true
        }

        test("keeps Dokka's own defaults for everything left unset") {
            val project = evaluated { docs -> docs.enabled.set(true) }
            val dokka = dokkaOf(project)

            dokka.moduleName.get() shouldBe "library"
        }

        test("hands the module name, output directory and copyright to Dokka") {
            val project = evaluated { docs ->
                docs.enabled.set(true)
                docs.moduleName.set("Library API")
                docs.outputDirectory.set("site/api")
                docs.copyright.set("Copyright Davils")
            }
            val dokka = dokkaOf(project)
            val html = dokka.dokkaPublications.getByName("html")
            val output = html.outputDirectory.get()

            dokka.moduleName.get() shouldBe "Library API"
            output.asFile.invariantSeparatorsPath shouldEndWith "build/site/api"
            footerOf(dokka).footerMessage.get() shouldBe "Copyright Davils"
        }
    }
})
