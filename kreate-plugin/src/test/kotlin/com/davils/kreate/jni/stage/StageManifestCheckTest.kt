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

import com.davils.kreate.freshDirectory
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File

private const val REVISION: String = "0123456789abcdef0123456789abcdef01234567"

private fun expectation(revision: String? = REVISION, isRelease: Boolean = true): StageExpectation =
    StageExpectation(
        platform = "linux-x86_64",
        group = "com.example",
        artifact = "sample",
        version = "1.0.0",
        revision = revision,
        isRelease = isRelease
    )

private fun stagedManifest(directory: File, version: String = "1.0.0", revision: String? = REVISION): StageManifest {
    val library = directory.resolve("libsample.so")
    return StageManifest(
        platform = "linux-x86_64",
        group = "com.example",
        artifact = "sample",
        version = version,
        revision = revision,
        digests = mapOf(library.name to LibraryDigest.sha256Of(library))
    )
}

class StageManifestCheckTest : FunSpec({
    val workspace = tempdir()

    fun stagedDirectory(): File {
        val directory = workspace.freshDirectory("staged")
        directory.resolve("libsample.so").writeText("native code")
        directory.resolve(STAGE_MANIFEST_FILE_NAME).writeText("stands in for the manifest")
        return directory
    }

    context("StageManifestCheck") {
        test("accepts what this build would have staged itself") {
            val directory = stagedDirectory()

            val review = StageManifestCheck.review(stagedManifest(directory), expectation(), directory)

            review shouldBe StageReview(problems = emptyList(), warnings = emptyList())
        }

        test("names every field that belongs to another build") {
            val directory = stagedDirectory()
            val manifest = stagedManifest(directory, version = "0.9.0").copy(platform = "macos-aarch64")

            val review = StageManifestCheck.review(manifest, expectation(), directory)

            review.problems shouldContainExactly listOf(
                "was staged for the platform \"macos-aarch64\", but this build publishes \"linux-x86_64\"",
                "was staged for the version \"0.9.0\", but this build publishes \"1.0.0\""
            )
        }

        test("fails a release built from another commit") {
            val directory = stagedDirectory()
            val manifest = stagedManifest(directory, revision = "fedcba")

            val review = StageManifestCheck.review(manifest, expectation(), directory)

            review.problems.single() shouldContain "was built from the commit \"fedcba\""
        }

        test("only warns about another commit for a snapshot") {
            val directory = stagedDirectory()
            val manifest = stagedManifest(directory, revision = "fedcba")

            val review = StageManifestCheck.review(manifest, expectation(isRelease = false), directory)

            review.problems.shouldBeEmpty()
            review.warnings.single() shouldContain "was built from the commit \"fedcba\""
        }

        test("warns when the commit cannot be compared") {
            val directory = stagedDirectory()

            val review = StageManifestCheck.review(stagedManifest(directory), expectation(revision = null), directory)

            review.problems.shouldBeEmpty()
            review.warnings.single() shouldContain "cannot be matched to a commit"
        }

        test("finds a library that changed after it was staged") {
            val directory = stagedDirectory()
            val manifest = stagedManifest(directory)
            directory.resolve("libsample.so").appendText(" replaced")

            val review = StageManifestCheck.review(manifest, expectation(), directory)

            review.problems.single() shouldContain "\"libsample.so\" with a SHA-256 its manifest does not give"
        }

        test("finds files the manifest does not list and files it lists that are gone") {
            val directory = stagedDirectory()
            val manifest = stagedManifest(directory)
            directory.resolve("libsample.so").renameTo(directory.resolve("libplanted.so"))

            val review = StageManifestCheck.review(manifest, expectation(), directory)

            review.problems shouldContainExactly listOf(
                "holds \"libplanted.so\", which its manifest does not list",
                "lists \"libsample.so\", which is not there"
            )
        }

        test("finds a directory, whose content would never be checked") {
            val directory = stagedDirectory()
            directory.resolve("nested").mkdirs()

            val review = StageManifestCheck.review(stagedManifest(directory), expectation(), directory)

            review.problems.single() shouldContain "holds the directory \"nested\""
        }
    }
})
