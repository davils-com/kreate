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
package com.davils.kreate.governance.catalog

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private val CATALOG: String = """
    # The versions of the build
    [versions]
    kotlin = "2.4.21" # pinned
    jackson = "2.18.0"

    [libraries]
    kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
    kotlin-reflect = { group = "org.jetbrains.kotlin", name = "kotlin-reflect", version.ref = "kotlin" }
    jackson = { module = "com.fasterxml.jackson.core:jackson-databind", version.ref = "jackson" }
    guava = "com.google.guava:guava:33.0.0-jre"
    lang = { module = "org.apache.commons:commons-lang3", version = "3.14.0" }
    rich = { module = "org.example:rich", version = { strictly = "1.0" } }

    [plugins]
    kotlin = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
""".trimIndent()

private fun VersionCatalog.named(alias: String): CatalogLibrary = libraryNamed(alias).shouldNotBeNull()

class VersionCatalogTest : FunSpec({
    val catalog = VersionCatalog(CATALOG)

    context("reading") {
        test("finds every library notation") {
            catalog.libraries.map { library -> library.alias } shouldBe
                listOf("kotlin-stdlib", "kotlin-reflect", "jackson", "guava", "lang", "rich")
        }

        test("resolves a version reference") {
            val jackson = catalog.named("jackson")

            catalog.effectiveVersion(jackson) shouldBe "2.18.0"
        }

        test("reads a version from the string notation and from a table") {
            catalog.effectiveVersion(catalog.named("guava")) shouldBe "33.0.0-jre"
            catalog.effectiveVersion(catalog.named("lang")) shouldBe "3.14.0"
        }

        test("knows a rich version cannot be raised") {
            catalog.effectiveVersion(catalog.named("rich")) shouldBe null
        }

        test("knows who shares a version reference") {
            catalog.sharersOf(catalog.named("kotlin-stdlib")) shouldBe listOf("kotlin-reflect")
        }
    }

    context("raising a version") {
        test("rewrites the reference and keeps the rest, comments included") {
            val updated = catalog.withVersion(catalog.named("jackson"), "2.18.6")

            updated shouldBe CATALOG.replace("jackson = \"2.18.0\"", "jackson = \"2.18.6\"")
        }

        test("rewrites the string notation") {
            val updated = catalog.withVersion(catalog.named("guava"), "33.4.0-jre")

            updated shouldContain "guava = \"com.google.guava:guava:33.4.0-jre\""
        }

        test("rewrites an inline version") {
            val updated = catalog.withVersion(catalog.named("lang"), "3.18.0")

            updated shouldContain "version = \"3.18.0\" }"
        }

        test("refuses what is not an exact version") {
            shouldThrow<IllegalArgumentException> {
                catalog.withVersion(catalog.named("lang"), "3.+\"\n[evil]")
            }
        }
    }

    context("adding a library") {
        test("adds a version and a library entry to their sections") {
            val updated = VersionCatalog(catalog.withLibrary("okio", "com.squareup.okio", "okio", "3.9.0"))

            val okio = updated.named("okio")
            okio.module shouldBe "com.squareup.okio:okio"
            updated.effectiveVersion(okio) shouldBe "3.9.0"
            updated.libraries.size shouldBe 7
        }

        test("writes a literal version when the version alias is taken") {
            val updated = catalog.withLibrary("kotlin", "org.example", "kotlin-thing", "1.0")

            updated shouldContain "kotlin = { module = \"org.example:kotlin-thing\", version = \"1.0\" }"
        }

        test("creates the sections of an empty catalog") {
            val updated = VersionCatalog("").withLibrary("okio", "com.squareup.okio", "okio", "3.9.0")

            val expected = """
                [versions]
                okio = "3.9.0"

                [libraries]
                okio = { module = "com.squareup.okio:okio", version.ref = "okio" }

            """.trimIndent()
            updated shouldBe expected
        }

        test("refuses a taken alias, a bad module and a bad alias") {
            shouldThrow<IllegalArgumentException> { catalog.withLibrary("guava", "a", "b", "1") }
            shouldThrow<IllegalArgumentException> { catalog.withLibrary("fine", "a\"b", "c", "1") }
            shouldThrow<IllegalArgumentException> { catalog.withLibrary("Bad Alias", "a", "b", "1") }
        }
    }

    context("names") {
        test("derive an alias from an artifact") {
            CatalogNames.aliasFor("jackson-databind") shouldBe "jackson-databind"
            CatalogNames.aliasFor("HikariCP") shouldBe "hikaricp"
            CatalogNames.aliasFor("2fa_lib") shouldBe "lib-2fa-lib"
        }

        test("turn an alias into an accessor") {
            CatalogNames.accessorOf("kotlin-stdlib") shouldBe "kotlin.stdlib"
        }
    }
})
