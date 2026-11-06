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
package com.davils.kreate.trivy.provision

import com.davils.kreate.archive.gzip
import com.davils.kreate.archive.tarOf
import com.davils.kreate.archive.zipOf
import com.davils.kreate.freshDirectory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import java.io.File

class TrivyArchiveTest : FunSpec({
    val workspace = tempdir()

    fun extracted(archiveBytes: ByteArray, asset: TrivyAsset): ByteArray {
        val directory = workspace.freshDirectory("extract")
        val archive = directory.resolve("archive")
        archive.writeBytes(archiveBytes)
        val target = File(directory, "binary")

        TrivyArchive.extract(archive.toPath(), asset, target.toPath())

        return target.readBytes()
    }

    context("extracting") {
        test("skips everything before the binary in a tar archive, across block padding") {
            val tar = gzip(tarOf("README" to ByteArray(700), "trivy" to "binary".toByteArray()))

            extracted(tar, TrivyAsset("Linux-64bit", "tar.gz", "trivy")) shouldBe "binary".toByteArray()
        }

        test("does not take a nested file of the same name") {
            val tar = gzip(tarOf("contrib/trivy" to "decoy".toByteArray()))

            shouldThrow<TrivyUnavailableException> {
                extracted(tar, TrivyAsset("Linux-64bit", "tar.gz", "trivy"))
            }
        }

        test("reads a zip archive for Windows") {
            val zip = zipOf("LICENSE" to "text".toByteArray(), "trivy.exe" to "binary".toByteArray())

            extracted(zip, TrivyAsset("windows-64bit", "zip", "trivy.exe")) shouldBe "binary".toByteArray()
        }
    }
})
