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
import java.io.File
import java.time.Instant

class LocalLibraryTest : FunSpec({

    fun library(publishedAt: String?) = LocalLibrary(
        library = "core",
        group = "com.example",
        repository = File("/workspace/core"),
        version = "1.0.0-SNAPSHOT",
        publishedAt = publishedAt,
        kreateVersion = "3.2.0",
        modules = listOf(LocalModule(group = "com.example", name = "core"))
    )

    context("publishedAtInstant") {

        test("parses the recorded ISO-8601 instant") {
            library("2026-09-23T14:02:11Z").publishedAtInstant() shouldBe Instant.parse("2026-09-23T14:02:11Z")
        }

        test("is unknown when nothing was recorded") {
            library(null).publishedAtInstant() shouldBe null
        }

        test("is unknown rather than failing when the record cannot be read") {
            library("yesterday afternoon").publishedAtInstant() shouldBe null
        }
    }
})
