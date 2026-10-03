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

package com.davils.kreate.settings.local

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.gradle.StartParameter
import org.gradle.api.Transformer
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

class LocalPublishRequestTest : FunSpec({

    fun publishProperty(value: String?): Provider<String> {
        val property = providerOf(value)
        every { property.map(any<Transformer<Boolean, String>>()) } answers {
            val transformer = firstArg<Transformer<Boolean, String>>()
            providerOf(value?.let(transformer::transform))
        }
        return property
    }

    fun providers(value: String? = null): ProviderFactory {
        val providers = mockk<ProviderFactory>()
        every { providers.gradleProperty(LOCAL_PUBLISH_PROPERTY) } returns publishProperty(value)
        return providers
    }

    fun build(vararg taskNames: String, parent: Gradle? = null): Gradle {
        val startParameter = mockk<StartParameter>()
        every { startParameter.taskNames } returns taskNames.toList()
        val gradle = mockk<Gradle>()
        every { gradle.parent } returns parent
        every { gradle.startParameter } returns startParameter
        return gradle
    }

    context("requestsLocalPublish") {

        context("the publish property") {

            test("requests a publication when set to true in any case") {
                val gradle = mockk<Gradle>()

                requestsLocalPublish(gradle, providers("TRUE")) shouldBe true
                verify(exactly = 0) { gradle.startParameter }
            }

            test("falls through to the requested tasks when set to anything else") {
                requestsLocalPublish(build("assemble"), providers("yes")) shouldBe false
            }
        }

        context("the requested tasks") {

            test("request a publication when the publish task is among them") {
                requestsLocalPublish(build("clean", LocalTaskNames.PUBLISH), providers()) shouldBe true
            }

            test("match a task path by its last segment") {
                requestsLocalPublish(build(":library:${LocalTaskNames.PUBLISH}"), providers()) shouldBe true
            }

            test("are not matched by a task that merely starts with the same name") {
                requestsLocalPublish(build(LocalTaskNames.PUBLISH_ALL), providers()) shouldBe false
            }

            test("are read from the root build when an included build asks") {
                val root = build(LocalTaskNames.PUBLISH)
                val included = build(parent = root)

                requestsLocalPublish(included, providers()) shouldBe true
            }
        }
    }
})
