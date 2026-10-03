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

package com.davils.kreate.detekt

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreatePlugin
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.gradle.testfixtures.ProjectBuilder

class DetektExtensionTest : FunSpec({
    fun detekt(): DetektExtension {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(KreatePlugin::class.java)
        val kreate = project.extensions.getByType(KreateExtension::class.java)
        return kreate.project.detekt
    }

    context("DetektExtension") {
        test("only the human readable reports are required by default") {
            val reports = detekt().reports

            reports.checkstyle.required.get() shouldBe false
            reports.html.required.get() shouldBe true
            reports.markdown.required.get() shouldBe true
            reports.sarif.required.get() shouldBe false
        }

        test("the reports block configures every report format") {
            val extension = detekt()
            extension.reports {
                checkstyle { required.set(true) }
                html { required.set(false) }
                markdown { required.set(false) }
                sarif { required.set(true) }
            }
            val reports = extension.reports

            reports.checkstyle.required.get() shouldBe true
            reports.html.required.get() shouldBe false
            reports.markdown.required.get() shouldBe false
            reports.sarif.required.get() shouldBe true
        }
    }
})
