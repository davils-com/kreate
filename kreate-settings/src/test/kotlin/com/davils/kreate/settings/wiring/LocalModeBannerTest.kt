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

package com.davils.kreate.settings.wiring

import com.davils.kreate.settings.local.LocalLibrary
import com.davils.kreate.settings.local.LocalMode
import com.davils.kreate.settings.local.LocalModule
import com.davils.kreate.settings.local.LocalWorkspace
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.io.File
import java.time.Duration
import java.time.Instant

class LocalModeBannerTest : FunSpec({

    val repository = File("/home/dev/.m2/repository")

    val logger = mockk<Logger>()
    val lines = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    beforeSpec {
        mockkStatic(Logging::class)
        every { Logging.getLogger(Settings::class.java) } returns logger
        every { logger.lifecycle(capture(lines)) } just runs
        every { logger.warn(capture(warnings)) } just runs
    }

    afterSpec {
        unmockkStatic(Logging::class)
    }

    beforeTest {
        lines.clear()
        warnings.clear()
    }

    fun library(
        name: String,
        publishedAt: String? = null,
        modules: List<String> = listOf(name)
    ) = LocalLibrary(
        library = name,
        group = "com.example",
        repository = File("/workspace/$name"),
        version = "1.0.0-SNAPSHOT",
        publishedAt = publishedAt,
        kreateVersion = "3.2.0",
        modules = modules.map { LocalModule(group = "com.example", name = it) }
    )

    fun announce(vararg libraries: LocalLibrary) {
        val mode = LocalMode.Active(LocalWorkspace(libraries.toList()))
        mockk<Settings>().announceLocalMode(mode, repository)
    }

    fun publishedAgo(elapsed: Duration): String {
        val instant = Instant.now() - elapsed
        return instant.toString()
    }

    fun rowOf(name: String): String = lines.single { line -> line.trimStart().startsWith(name) }

    context("the local mode banner") {

        context("the headline") {

            test("counts the libraries and names the repository") {
                announce(library("core"), library("net"))

                lines shouldContain "Kreate local mode is ON — 2 libraries resolved from $repository:"
            }

            test("uses the singular for a single library") {
                announce(library("core"))

                lines shouldContain "Kreate local mode is ON — 1 library resolved from $repository:"
            }

            test("explains how to switch local mode off and clear it") {
                announce(library("core"))

                lines.joinToString("\n") shouldContain "-Pkreate.local=false"
                lines.joinToString("\n") shouldContain "./gradlew kreateLocalClean"
            }
        }

        context("a library row") {

            test("shows the version, the module count and the producing checkout") {
                announce(library("core", modules = listOf("core", "core-jvm")))

                rowOf("core") shouldContain "1.0.0-SNAPSHOT"
                rowOf("core") shouldContain "2 module(s)"
                rowOf("core") shouldEndWith File("/workspace/core").toString()
            }

            test("reports an unknown publication time") {
                announce(library("core"), library("net", publishedAt = "not a timestamp"))

                rowOf("core") shouldContain "unknown"
                rowOf("net") shouldContain "unknown"
            }

            test("reports a publication of the last minute as just now") {
                announce(library("core", publishedAt = publishedAgo(Duration.ofSeconds(5))))

                rowOf("core") shouldContain "just now"
            }

            test("reports a recent publication in minutes") {
                announce(library("core", publishedAt = publishedAgo(Duration.ofMinutes(5))))

                rowOf("core") shouldContain "5 min ago"
            }

            test("reports a publication of today in hours") {
                announce(library("core", publishedAt = publishedAgo(Duration.ofHours(3))))

                rowOf("core") shouldContain "3 h ago"
            }

            test("reports an older publication in days") {
                announce(library("core", publishedAt = publishedAgo(Duration.ofDays(2))))

                rowOf("core") shouldContain "2 d ago"
            }
        }

        context("coordinates claimed twice") {

            test("are warned about by name") {
                announce(library("core", modules = listOf("shared")), library("net", modules = listOf("shared")))

                warnings shouldHaveSize 1
                warnings.single() shouldContain "1 coordinate(s)"
                warnings.single() shouldContain "com.example:shared"
            }

            test("cause no warning when every coordinate has one owner") {
                announce(library("core"), library("net"))

                warnings.shouldBeEmpty()
            }
        }
    }
})
