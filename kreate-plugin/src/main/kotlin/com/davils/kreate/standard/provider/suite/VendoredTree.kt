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
package com.davils.kreate.standard.provider.suite

import com.davils.kore.core.outcome.Outcome
import com.davils.kreate.digest.Sha256
import com.davils.kreate.standard.model.LookupFailure
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

private const val MAX_FILES: Int = 200_000

private const val MAX_TOTAL_BYTES: Long = 2L * 1024 * 1024 * 1024

private const val BUFFER_BYTES: Int = 64 * 1024

private const val SEPARATOR: Int = 0

private const val SHA_1: String = "SHA-1"

internal class VendoredTree(val digest: String, val files: List<VendoredFile>) {
    companion object {
        fun read(directory: Path, shown: String): Outcome<VendoredTree, LookupFailure> {
            if (!directory.isDirectory(LinkOption.NOFOLLOW_LINKS)) {
                return Outcome.Failure(LookupFailure.Mismatch("The vendored directory $shown does not exist."))
            }
            return try {
                treeOf(directory, shown)
            } catch (cause: IOException) {
                Outcome.Failure(LookupFailure.Unavailable("$shown could not be read: ${cause.message.orEmpty()}"))
            }
        }

        private fun treeOf(directory: Path, shown: String): Outcome<VendoredTree, LookupFailure> {
            val entries = Files.walk(directory).use { stream -> stream.toList() }
            val others = entries.filterNot { entry -> entry.isDirectory(LinkOption.NOFOLLOW_LINKS) }
            val irregular = others.firstOrNull { entry -> !entry.isRegularFile(LinkOption.NOFOLLOW_LINKS) }
            if (irregular != null) {
                val path = relativeOf(directory, irregular)
                return Outcome.Failure(LookupFailure.Mismatch("$shown holds $path, which is no regular file."))
            }
            if (others.size > MAX_FILES) return tooLarge(shown, "more than $MAX_FILES files")
            val total = others.sumOf { file -> Files.size(file) }
            if (total > MAX_TOTAL_BYTES) return tooLarge(shown, "more than $MAX_TOTAL_BYTES bytes")
            val byPath = others.associateBy { file -> relativeOf(directory, file) }
            val tree = Sha256.digest()
            val files = byPath.keys.sorted().map { path -> fileOf(path, byPath.getValue(path), tree) }
            return Outcome.Success(VendoredTree("sha256:" + HexFormat.of().formatHex(tree.digest()), files))
        }

        private fun tooLarge(shown: String, what: String): Outcome<VendoredTree, LookupFailure> =
            Outcome.Failure(LookupFailure.Unavailable("$shown holds $what, too many to check."))

        private fun relativeOf(directory: Path, file: Path): String =
            directory.relativize(file).invariantSeparatorsPathString

        private fun fileOf(path: String, file: Path, tree: MessageDigest): VendoredFile {
            val size = Files.size(file)
            val content = Sha256.digest()
            val blob = MessageDigest.getInstance(SHA_1)
            tree.update(path.toByteArray(Charsets.UTF_8))
            tree.update(SEPARATOR.toByte())
            tree.update(size.toString().toByteArray(Charsets.US_ASCII))
            tree.update(SEPARATOR.toByte())
            blob.update("blob $size".toByteArray(Charsets.US_ASCII))
            blob.update(SEPARATOR.toByte())
            val buffer = ByteArray(BUFFER_BYTES)
            Files.newInputStream(file).use { stream ->
                var read = stream.read(buffer)
                while (read >= 0) {
                    tree.update(buffer, 0, read)
                    content.update(buffer, 0, read)
                    blob.update(buffer, 0, read)
                    read = stream.read(buffer)
                }
            }
            val hex = HexFormat.of()
            return VendoredFile(path, hex.formatHex(content.digest()), hex.formatHex(blob.digest()))
        }
    }
}
