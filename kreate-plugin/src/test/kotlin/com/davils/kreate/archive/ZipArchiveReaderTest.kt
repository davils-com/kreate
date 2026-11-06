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

package com.davils.kreate.archive

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

private const val MIB: Int = 1024 * 1024

private const val COMPRESSED_SIZE_FIELD: Int = 20

private const val SIZE_FIELD: Int = 24

private const val CRC_FIELD: Int = 16

private const val FLAGS_FIELD: Int = 8

private const val METHOD_FIELD: Int = 10

private const val VERSION_MADE_BY_FIELD: Int = 5

private const val EXTERNAL_ATTRIBUTES_FIELD: Int = 38

private fun readerWith(limits: ArchiveLimits = ArchiveLimits()): ZipArchiveReader =
    ZipArchiveReader(ArchiveBudget(limits))

private fun read(archive: ByteArray): List<ArchiveEntry> = readerWith().readAll(archive)

private fun shouldBeMalformed(archive: ByteArray, expectedMessage: String) {
    val failure = shouldThrow<ArchiveException.Malformed> { read(archive) }

    failure.message.orEmpty() shouldContain expectedMessage
}

class ZipArchiveReaderTest : FunSpec({
    val plain = zipOf("a.txt" to "hello".toByteArray())

    context("reading") {
        test("reads deflated entries") {
            val entry = read(zipOf("a.txt" to "hello".toByteArray())).single()

            entry.name shouldBe "a.txt"
            entry.bytes.toString(Charsets.UTF_8) shouldBe "hello"
            entry.kind shouldBe ArchiveEntryKind.FILE
            entry.isFile shouldBe true
            entry.pathIssue.shouldBeNull()
        }

        test("reads stored entries") {
            read(storedZipOf("a.txt", "hello".toByteArray())).single().bytes shouldBe "hello".toByteArray()
        }

        test("reads empty files") {
            val entry = read(zipOf("empty" to ByteArray(0))).single()

            entry.bytes.toList().shouldBeEmpty()
        }

        test("agrees with the JVM on a real JAR") {
            val location = Unit::class.java.protectionDomain.codeSource.location
            val jar = File(location.toURI())

            val names = ZipFile(jar).use { zip ->
                val entries = zip.entries().toList()
                entries.map { entry -> entry.name }
            }

            read(jar.readBytes()).map { entry -> entry.name } shouldBe names
        }

        test("reads an archive without entries") {
            read(zipOf()).shouldBeEmpty()
        }

        test("recognises directories") {
            val entry = read(zipOf("dir/" to ByteArray(0))).single()

            entry.kind shouldBe ArchiveEntryKind.DIRECTORY
            entry.isFile shouldBe false
        }

        test("keeps duplicates, which a map based reader would merge") {
            val first = zipOf("a" to byteArrayOf(1))
            val duplicates = duplicateNameArchive()

            read(first) shouldHaveSize 1
            read(duplicates).map { it.name } shouldBe listOf("a", "a")
        }

        test("reads an archive with a comment") {
            val bytes = ByteArrayOutputStream()
            ZipOutputStream(bytes).use { zip ->
                zip.setComment("a comment")
                zip.putNextEntry(ZipEntry("a"))
                zip.closeEntry()
            }

            read(bytes.toByteArray()) shouldHaveSize 1
        }

        test("reads an archive behind a launcher script, like an executable JAR") {
            val script = "#!/bin/sh\nexec java -jar \"$0\"\n".toByteArray()

            read(script + plain).single().name shouldBe "a.txt"
        }

        test("reads non-Unix archives without a mode") {
            read(plain).single().mode shouldBe 0
        }

        test("reads the mode and kind written by a Unix host") {
            val header = plain.centralHeader()
            val unix = plain
                .withByte(header + VERSION_MADE_BY_FIELD, 3)
                .withInt(header + EXTERNAL_ATTRIBUTES_FIELD, 0x81ED shl 16)

            val entry = read(unix).single()

            entry.mode shouldBe 0x1ED
            entry.kind shouldBe ArchiveEntryKind.FILE
        }

        test("reads a Unix directory without a trailing slash") {
            val header = plain.centralHeader()
            val unix = plain
                .withByte(header + VERSION_MADE_BY_FIELD, 3)
                .withInt(header + EXTERNAL_ATTRIBUTES_FIELD, 0x41ED shl 16)

            read(unix).single().kind shouldBe ArchiveEntryKind.DIRECTORY
        }

        test("reports a Unix special file") {
            val header = plain.centralHeader()
            val unix = plain
                .withByte(header + VERSION_MADE_BY_FIELD, 3)
                .withInt(header + EXTERNAL_ATTRIBUTES_FIELD, 0x21A4 shl 16)

            read(unix).single().kind shouldBe ArchiveEntryKind.SPECIAL
        }

        test("reports a symbolic link and its target") {
            val link = storedZipOf("link", "../../etc/passwd".toByteArray())
            val header = link.centralHeader()
            val unix = link
                .withByte(header + VERSION_MADE_BY_FIELD, 3)
                .withInt(header + EXTERNAL_ATTRIBUTES_FIELD, 0xA1FF shl 16)

            val entry = read(unix).single()

            entry.kind shouldBe ArchiveEntryKind.SYMBOLIC_LINK
            entry.linkTarget shouldBe "../../etc/passwd"
            entry.pathIssue shouldBe ArchivePathIssue.TRAVERSAL
        }
    }

    context("traversal") {
        mapOf(
            "../evil" to ArchivePathIssue.TRAVERSAL,
            "a/../../evil" to ArchivePathIssue.TRAVERSAL,
            "a\\..\\..\\evil" to ArchivePathIssue.TRAVERSAL,
            "/etc/passwd" to ArchivePathIssue.ABSOLUTE,
            "\\\\server\\share" to ArchivePathIssue.ABSOLUTE,
            "C:\\Windows\\evil" to ArchivePathIssue.ABSOLUTE,
            "evil\u0000.txt" to ArchivePathIssue.CONTROL_CHARACTER
        ).forEach { (name, issue) ->
            test("flags the entry name ${name.replace('\u0000', '?')}") {
                read(zipOf(name to ByteArray(1))).single().pathIssue shouldBe issue
            }
        }
    }

    context("zip bombs") {
        test("refuses a bomb before inflating it") {
            val bomb = zipOf("bomb" to ByteArray(32 * MIB))
            val budget = ArchiveBudget(ArchiveLimits(maxExtractedBytes = MIB.toLong()))

            shouldThrow<ArchiveException.LimitExceeded> { ZipArchiveReader(budget).readAll(bomb) }

            budget.extractedBytes shouldBeLessThan MIB.toLong() + 1
        }

        test("counts the budget across entries, not per entry") {
            val entries = (1..10).map { index -> "part$index" to ByteArray(MIB / 2) }
            val archive = zipOf(*entries.toTypedArray())
            val limits = ArchiveLimits(maxExtractedBytes = 2L * MIB)

            shouldThrow<ArchiveException.LimitExceeded> { readerWith(limits).readAll(archive) }
        }

        test("accepts an archive of exactly the budget") {
            val archive = zipOf("a" to ByteArray(1024))
            val budget = ArchiveBudget(ArchiveLimits(maxExtractedBytes = 1024))

            ZipArchiveReader(budget).readAll(archive) shouldHaveSize 1
            budget.extractedBytes shouldBe 1024L
        }

        test("does not trust a declared size that hides the real one") {
            val archive = zipOf("bomb" to ByteArray(MIB))
            val forged = archive.withInt(archive.centralHeader() + SIZE_FIELD, 16)

            shouldBeMalformed(forged, "longer than it declares")
        }

        test("charges a declared size up front") {
            val archive = zipOf("a" to ByteArray(16))
            val forged = archive.withInt(archive.centralHeader() + SIZE_FIELD, MIB)
            val limits = ArchiveLimits(maxExtractedBytes = 1024)

            shouldThrow<ArchiveException.LimitExceeded> { readerWith(limits).readAll(forged) }
        }

        test("refuses a declared size no deflate stream can reach") {
            val archive = zipOf("a" to ByteArray(16))
            val forged = archive.withInt(archive.centralHeader() + SIZE_FIELD, 256 * MIB)

            val failure = shouldThrow<ArchiveException.Malformed> { read(forged) }

            failure.message.orEmpty() shouldContain "impossible"
        }

        test("refuses a declared size longer than the content") {
            val archive = zipOf("a" to ByteArray(16))
            val forged = archive.withInt(archive.centralHeader() + SIZE_FIELD, 17)

            shouldBeMalformed(forged, "shorter than it declares")
        }

        test("charges entries that share their compressed data") {
            val overlapping = overlappingEntriesArchive()
            val limits = ArchiveLimits(maxExtractedBytes = 1024)

            shouldThrow<ArchiveException.LimitExceeded> { readerWith(limits).readAll(overlapping) }
        }

        test("refuses an entry too large to hold in memory") {
            val archive = storedZipOf("a", ByteArray(16))
            val header = archive.centralHeader()
            val forged = archive
                .withInt(header + SIZE_FIELD, Int.MIN_VALUE)
                .withInt(header + COMPRESSED_SIZE_FIELD, 16)
            val limits = ArchiveLimits(maxExtractedBytes = Long.MAX_VALUE)

            shouldThrow<ArchiveException.LimitExceeded> { readerWith(limits).readAll(forged) }
        }
    }

    context("entry limit") {
        test("refuses too many entries") {
            val entries = (1..5).map { index -> "e$index" to ByteArray(0) }
            val archive = zipOf(*entries.toTypedArray())

            shouldThrow<ArchiveException.LimitExceeded> { readerWith(ArchiveLimits(maxEntries = 4)).readAll(archive) }
        }

        test("accepts exactly the limit") {
            val entries = (1..4).map { index -> "e$index" to ByteArray(0) }
            val archive = zipOf(*entries.toTypedArray())

            readerWith(ArchiveLimits(maxEntries = 4)).readAll(archive) shouldHaveSize 4
        }
    }

    context("nested archives") {
        test("reads a nested archive") {
            val inner = zipOf("inner.txt" to "x".toByteArray())
            val outer = zipOf("inner.jar" to inner)
            val reader = readerWith()
            val names = mutableListOf<String>()

            reader.forEachEntry(outer) { entry ->
                reader.forEachNestedEntry(entry.bytes) { nested -> names.add(nested.name) }
            }

            names shouldBe listOf("inner.txt")
        }

        test("shares one budget across nested archives") {
            val inner = zipOf("data" to ByteArray(MIB))
            val outer = zipOf(*(1..4).map { index -> "lib$index.jar" to inner }.toTypedArray())
            val budget = ArchiveBudget(ArchiveLimits(maxExtractedBytes = 3L * MIB))
            val reader = ZipArchiveReader(budget)

            shouldThrow<ArchiveException.LimitExceeded> {
                reader.forEachEntry(outer) { entry -> reader.forEachNestedEntry(entry.bytes) { } }
            }
        }

        test("passes a budget that each single archive would pass only when it is per archive") {
            val inner = zipOf("data" to ByteArray(MIB))
            val outer = zipOf("a.jar" to inner, "b.jar" to inner)
            val limits = ArchiveLimits(maxExtractedBytes = 3L * MIB / 2)
            val perArchive = ZipArchiveReader(ArchiveBudget(limits))
            val shared = ZipArchiveReader(ArchiveBudget(limits))

            perArchive.readAll(inner) shouldHaveSize 1
            shouldThrow<ArchiveException.LimitExceeded> {
                shared.forEachEntry(outer) { entry -> shared.forEachNestedEntry(entry.bytes) { } }
            }
        }

        test("shares the entry count across nested archives") {
            val inner = zipOf("x" to ByteArray(0), "y" to ByteArray(0))
            val outer = zipOf("a.jar" to inner, "b.jar" to inner)
            val reader = readerWith(ArchiveLimits(maxEntries = 5))

            shouldThrow<ArchiveException.LimitExceeded> {
                reader.forEachEntry(outer) { entry -> reader.forEachNestedEntry(entry.bytes) { } }
            }
        }

        test("stops at the nesting limit") {
            val level0 = zipOf("x" to ByteArray(0))
            val level1 = zipOf("n" to level0)
            val level2 = zipOf("n" to level1)
            val reader = readerWith(ArchiveLimits(maxNestingDepth = 1))

            shouldThrow<ArchiveException.LimitExceeded> {
                reader.forEachEntry(level2) { outer ->
                    reader.forEachNestedEntry(outer.bytes) { middle ->
                        reader.forEachNestedEntry(middle.bytes) { }
                    }
                }
            }
        }
    }

    context("malformed archives") {
        test("refuses bytes that are no archive") {
            shouldBeMalformed("just text, long enough to hold a record".toByteArray(), "no ZIP end record")
        }

        test("refuses an empty input") {
            shouldBeMalformed(ByteArray(0), "no ZIP end record")
        }

        test("refuses a truncated archive") {
            shouldBeMalformed(plain.copyOf(plain.size - 1), "no ZIP end record")
        }

        test("refuses trailing data, because the end record can no longer be trusted") {
            shouldBeMalformed(plain + ByteArray(8), "no ZIP end record")
        }

        test("does not stop silently at garbage between the entries") {
            val archive = zipOf("first" to byteArrayOf(1), "hidden" to byteArrayOf(2))
            val secondLocalHeader = 30 + "first".length + 3 + 16
            val hiddenByGarbage = archive.withInt(secondLocalHeader, 0x12345678)

            shouldBeMalformed(hiddenByGarbage, "no local header")
        }

        test("refuses a local header that contradicts the central directory") {
            val nameInLocalHeader = 30

            shouldBeMalformed(plain.withByte(nameInLocalHeader, 'z'.code), "contradicts the central directory")
        }

        test("refuses a local method that contradicts the central directory") {
            val methodInLocalHeader = 8

            shouldBeMalformed(plain.withShort(methodInLocalHeader, 0), "contradicts the central directory")
        }

        test("refuses a wrong checksum") {
            val forged = plain.withInt(plain.centralHeader() + CRC_FIELD, 1)

            val failure = shouldThrow<ArchiveException.Malformed> { read(forged) }

            failure.message.orEmpty() shouldContain "checksum"
        }

        test("refuses encrypted entries") {
            val forged = plain.withShort(plain.centralHeader() + FLAGS_FIELD, 1)

            val failure = shouldThrow<ArchiveException.Malformed> { read(forged) }

            failure.message.orEmpty() shouldContain "encrypted"
        }

        test("refuses unknown compression methods") {
            val forged = plain
                .withShort(plain.centralHeader() + METHOD_FIELD, 12)
                .withShort(8, 12)

            val failure = shouldThrow<ArchiveException.Malformed> { read(forged) }

            failure.message.orEmpty() shouldContain "method"
        }

        test("refuses corrupt compressed data") {
            val archive = zipOf("a" to ByteArray(4096) { index -> index.toByte() })
            val dataStart = 30 + 1

            val corrupted = (dataStart until dataStart + 8).fold(archive) { bytes, offset ->
                bytes.withByte(offset, 0xFF)
            }

            shouldBeMalformed(corrupted, "corrupt data")
        }

        test("refuses stored entries with inconsistent sizes") {
            val archive = storedZipOf("a", ByteArray(16))
            val forged = archive.withInt(archive.centralHeader() + SIZE_FIELD, 8)

            shouldBeMalformed(forged, "inconsistent sizes")
        }

        test("refuses a local header outside the archive") {
            val forged = plain.withInt(plain.centralHeader() + 42, 1_000_000)

            shouldBeMalformed(forged, "lies outside the archive")
        }

        test("refuses a compressed size that runs into the central directory") {
            val forged = plain.withInt(plain.centralHeader() + COMPRESSED_SIZE_FIELD, 1_000_000)

            shouldBeMalformed(forged, "lies outside the archive")
        }

        test("refuses a wrong entry count") {
            val endRecord = plain.endRecord()
            val forged = plain
                .withShort(endRecord + 8, 2)
                .withShort(endRecord + 10, 2)

            shouldBeMalformed(forged, "wrong entry count")
        }

        test("refuses an end record that contradicts itself") {
            shouldBeMalformed(plain.withShort(plain.endRecord() + 8, 2), "contradicts itself")
        }

        test("refuses a corrupt central header") {
            shouldBeMalformed(plain.withInt(plain.centralHeader(), 1), "central directory header is corrupt")
        }

        test("refuses a central directory larger than its archive") {
            shouldBeMalformed(plain.withInt(plain.endRecord() + 12, 1_000_000), "lies outside the archive")
        }

        test("refuses a central directory offset before the archive") {
            shouldBeMalformed(plain.withInt(plain.endRecord() + 16, 1_000_000), "lies outside the archive")
        }

        test("refuses a central directory that overruns its declared size") {
            val endRecord = plain.endRecord()
            val directorySize = 46 + "a.txt".length
            val directoryStart = endRecord - directorySize
            val forged = plain.withShort(plain.centralHeader() + 32, 3)

            forged.centralHeader() shouldBe directoryStart
            shouldBeMalformed(forged, "overruns its declared size")
        }

        test("refuses multi-disk archives") {
            shouldBeMalformed(plain.withShort(plain.endRecord() + 4, 1), "Multi-disk")
            shouldBeMalformed(plain.withShort(plain.endRecord() + 6, 1), "Multi-disk")
        }

        listOf(10 to 0xFFFF).forEach { (field, value) ->
            test("refuses the ZIP64 entry count marker") {
                val endRecord = plain.endRecord()
                val forged = plain.withShort(endRecord + 8, value).withShort(endRecord + field, value)

                val failure = shouldThrow<ArchiveException.Malformed> { read(forged) }

                failure.message.orEmpty() shouldContain "ZIP64"
            }
        }

        listOf(12, 16).forEach { field ->
            test("refuses the ZIP64 marker at offset $field of the end record") {
                val forged = plain.withInt(plain.endRecord() + field, -1)

                val failure = shouldThrow<ArchiveException.Malformed> { read(forged) }

                failure.message.orEmpty() shouldContain "ZIP64"
            }
        }

        listOf(20, 24, 42).forEach { field ->
            test("refuses the ZIP64 marker at offset $field of a central header") {
                val forged = plain.withInt(plain.centralHeader() + field, -1)

                val failure = shouldThrow<ArchiveException.Malformed> { read(forged) }

                failure.message.orEmpty() shouldContain "ZIP64"
            }
        }

        test("refuses a ZIP64 locator, which the JVM honours even without a marker value") {
            val endRecord = plain.endRecord()
            val locator = ByteArray(20)
            val beforeEnd = plain.copyOfRange(0, endRecord)
            val shifted = beforeEnd + locator.withInt(0, 0x07064B50) + plain.copyOfRange(endRecord, plain.size)

            val failure = shouldThrow<ArchiveException.Malformed> { read(shifted) }

            failure.message.orEmpty() shouldContain "ZIP64"
        }
    }
})

private fun duplicateNameArchive(): ByteArray {
    val archive = zipOf("a" to byteArrayOf(1), "b" to byteArrayOf(1))
    val secondLocalName = 30 + 1 + 3 + 16 + 30
    return archive
        .withByte(secondLocalName, 'a'.code)
        .withByte(archive.centralHeader(1) + 46, 'a'.code)
}

private fun overlappingEntriesArchive(): ByteArray {
    val entries = (1..8).map { index -> "e$index" to ByteArray(512) }
    val archive = zipOf(*entries.toTypedArray())
    val offsetField = 42
    return (1 until entries.size).fold(archive) { bytes, index ->
        val name = bytes.centralHeader(index) + 46
        bytes.withInt(bytes.centralHeader(index) + offsetField, 0).withByte(name + 1, '1'.code)
    }
}
