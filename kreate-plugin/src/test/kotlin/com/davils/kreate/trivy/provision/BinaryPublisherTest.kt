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

import com.davils.kreate.freshDirectory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.nio.file.AccessDeniedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.HexFormat

private val PUBLISHED: ByteArray = "#!/bin/sh\necho trivy\n".toByteArray()

private fun sha256(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return HexFormat.of().formatHex(digest.digest(bytes))
}

private fun fileKeyOf(file: Path): Any? {
    val attributes = Files.readAttributes(file, BasicFileAttributes::class.java)
    return attributes.fileKey()
}

private fun refusingMove(source: Path, target: Path) {
    throw AccessDeniedException(source.toString(), target.toString(), "The file is in use.")
}

class BinaryPublisherTest : FunSpec({
    val workspace = tempdir()

    fun staged(directory: File): Path {
        val stage = directory.resolve(".install")
        stage.mkdirs()
        val binary = stage.resolve("trivy")
        binary.writeBytes(PUBLISHED)
        return binary.toPath()
    }

    fun digestOf(target: Path): String {
        val digestFile = BinaryPublisher.digestFileOf(target)
        return Files.readString(digestFile)
    }

    context("publishing into an empty cache") {
        test("records the digest before the binary appears, so no reader sees one without the other") {
            val directory = workspace.freshDirectory("cache")
            val target = directory.resolve("trivy").toPath()
            var digestFirst = false
            val publisher = BinaryPublisher({}) { source, destination ->
                digestFirst = Files.isRegularFile(BinaryPublisher.digestFileOf(destination))
                Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE)
            }

            publisher.publish(staged(directory), target) shouldBe target

            digestFirst shouldBe true
            Files.readAllBytes(target) shouldBe PUBLISHED
            digestOf(target) shouldBe sha256(PUBLISHED)
        }
    }

    context("publishing over a binary that is already there") {
        test("keeps an identical binary in place, as Windows cannot replace one that runs") {
            val directory = workspace.freshDirectory("cache")
            val target = directory.resolve("trivy").toPath()
            Files.write(target, PUBLISHED)
            val before = fileKeyOf(target)

            BinaryPublisher({}, ::refusingMove).publish(staged(directory), target) shouldBe target

            fileKeyOf(target) shouldBe before
            digestOf(target) shouldBe sha256(PUBLISHED)
        }

        test("accepts the same binary that another build moved in at the same time") {
            val directory = workspace.freshDirectory("cache")
            val target = directory.resolve("trivy").toPath()
            val warnings = mutableListOf<String>()
            val publisher = BinaryPublisher(warnings::add) { source, destination ->
                Files.copy(source, destination)
                throw FileAlreadyExistsException(destination.toString())
            }

            publisher.publish(staged(directory), target) shouldBe target

            Files.readAllBytes(target) shouldBe PUBLISHED
            digestOf(target) shouldBe sha256(PUBLISHED)
            warnings.single() shouldContain "at the same time"
        }

        test("refuses to settle for a different binary it cannot replace") {
            val directory = workspace.freshDirectory("cache")
            val target = directory.resolve("trivy").toPath()
            Files.writeString(target, "tampered")

            val failure = shouldThrow<TrivyUnavailableException> {
                BinaryPublisher({}, ::refusingMove).publish(staged(directory), target)
            }

            failure.message.orEmpty() shouldContain "could not be replaced"
        }

        test("replaces a different binary where the file system allows it") {
            val directory = workspace.freshDirectory("cache")
            val target = directory.resolve("trivy").toPath()
            Files.writeString(target, "tampered")

            BinaryPublisher({}).publish(staged(directory), target)

            Files.readAllBytes(target) shouldBe PUBLISHED
            digestOf(target) shouldBe sha256(PUBLISHED)
        }
    }
})
