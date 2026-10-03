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

package com.davils.kreate.settings

import com.davils.kreate.settings.local.freshDirectory
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.gradle.testfixtures.ProjectBuilder

class KreateSettingsExtensionTest : FunSpec({

    val workspace = tempdir()

    fun extension(): KreateSettingsExtension {
        val builder = ProjectBuilder.builder()
        val project = builder.withProjectDir(workspace.freshDirectory()).build()
        return project.objects.newInstance(KreateSettingsExtension::class.java)
    }

    context("the kreateSettings extension") {

        context("without any configuration") {

            test("resolves locally published artifacts") {
                extension().enabled.get() shouldBe true
            }

            test("reports the injected repository as KreateLocal") {
                extension().repositoryName.get() shouldBe "KreateLocal"
            }

            test("recognises the common CI environment variables") {
                extension().ciEnvironmentVariables.get() shouldContainExactly
                    listOf("CI", "GITLAB_CI", "GITHUB_ACTIONS", "CI_PIPELINE_ID")
            }
        }

        context("once configured") {

            test("keeps what the settings script set") {
                val configured = extension()
                configured.enabled.set(false)
                configured.repositoryName.set("Sibling")
                configured.ciEnvironmentVariables.set(listOf("BUILDKITE"))

                configured.enabled.get() shouldBe false
                configured.repositoryName.get() shouldBe "Sibling"
                configured.ciEnvironmentVariables.get() shouldContainExactly listOf("BUILDKITE")
            }
        }
    }
})
