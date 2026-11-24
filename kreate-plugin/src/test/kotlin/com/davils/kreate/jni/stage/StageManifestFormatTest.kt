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

package com.davils.kreate.jni.stage

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val DIGEST: String = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"

private fun manifest(
    version: String = "1.0.0",
    revision: String? = "abc123",
    digests: Map<String, String> = mapOf("libsample.so" to DIGEST)
): StageManifest = StageManifest(
    platform = "linux-x86_64",
    group = "com.example",
    artifact = "sample",
    version = version,
    revision = revision,
    digests = digests
)

class StageManifestFormatTest : FunSpec({
    context("StageManifestFormat") {
        test("reads back what it writes") {
            val written = manifest()

            StageManifestFormat.parse(StageManifestFormat.render(written)) shouldBe written
        }

        test("keeps a backslash in a value") {
            val written = manifest(version = """1.0\0""")

            StageManifestFormat.parse(StageManifestFormat.render(written)).version shouldBe """1.0\0"""
        }

        test("writes no revision when Git could not name one") {
            val written = manifest(revision = null)

            val rendered = StageManifestFormat.render(written)

            rendered.contains("revision=") shouldBe false
            StageManifestFormat.parse(rendered).revision shouldBe null
        }

        test("refuses a value that would start a new line") {
            val failure = shouldThrow<StageManifestException> {
                StageManifestFormat.render(manifest(version = "1.0.0\nversion=2.0.0"))
            }

            failure.message shouldContain "control character"
        }

        test("refuses to list a file name it could not read back") {
            shouldThrow<StageManifestException> {
                StageManifestFormat.render(manifest(digests = mapOf("lib sample.so" to DIGEST)))
            }
        }

        test("rejects a manifest of an unknown format") {
            val rendered = StageManifestFormat.render(manifest())

            val failure = shouldThrow<StageManifestException> {
                StageManifestFormat.parse(rendered.replace("format=1", "format=2"))
            }

            failure.message shouldContain "\"2\""
        }

        test("rejects a manifest without a version") {
            val rendered = StageManifestFormat.render(manifest())
            val withoutVersion = rendered.lines().filterNot { line -> line.startsWith("version=") }

            val failure = shouldThrow<StageManifestException> {
                StageManifestFormat.parse(withoutVersion.joinToString("\n"))
            }

            failure.message shouldContain "version"
        }

        test("rejects a manifest that lists no library") {
            val failure = shouldThrow<StageManifestException> {
                StageManifestFormat.parse("format=1\nplatform=p\ngroup=g\nartifact=a\nversion=1")
            }

            failure.message shouldContain "no library"
        }

        test("rejects a digest that is no SHA-256") {
            val rendered = StageManifestFormat.render(manifest())

            shouldThrow<StageManifestException> {
                StageManifestFormat.parse(rendered.replace(DIGEST, "abc"))
            }
        }
    }
})
