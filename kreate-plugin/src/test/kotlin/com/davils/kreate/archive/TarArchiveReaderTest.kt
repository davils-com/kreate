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
import java.io.ByteArrayInputStream
import java.util.Base64

private const val BLOCK: Int = 512

private const val MIB: Int = 1024 * 1024

private fun terminator(): ByteArray = ByteArray(2 * BLOCK)

private fun read(archive: ByteArray, limits: ArchiveLimits = ArchiveLimits()): List<ArchiveEntry> =
    TarArchiveReader(ArchiveBudget(limits)).readAll(ByteArrayInputStream(archive))

private fun readGzip(archive: ByteArray, limits: ArchiveLimits = ArchiveLimits()): List<ArchiveEntry> {
    val entries = mutableListOf<ArchiveEntry>()
    TarArchiveReader(ArchiveBudget(limits)).forEachGzipEntry(ByteArrayInputStream(archive), entries::add)
    return entries
}

private val GNU_TAR_ARCHIVES: Map<String, String> = mapOf(
    "gnu" to "H4sIAAAAAAAAA+3WwQqDMAwG4D5Kn2Bra1Ofxw1kZU5HzQZ7+7mCFw/KDpE5/+8SaAsppAnl" +
        "FJ8vJcsMSqIcB9M4CMqSdVQU3n/WrQlklDbC98oePVdJa5W6jufOLe1v1Cm2VZJ+APCzzl3LKZ6OkjmW" +
        "+99M+t86a5UmyUuNdt7/Y/2rvq4PfG8kcuQJ7/1M/f2k/i74AvN/DRe+idQctqGJ7VU6R57/ZfnN/CcX" +
        "lHa8wud05/0PAAAAAAAAAAAAAAAAAP/jDX7WVd0AKAAA",
    "ustar" to "H4sIAAAAAAAAA+3WQQqDMBBA0RzFE7SJJuY8tiANtVpiWujtmwpuXChdRGr9bzNBAzMwzmDw" +
        "7vkSacnIGjPEaBqjUiijclMUWn+eK2mVFJlMXNfg0YfKx1J814W5e0vvN+rk2sqn/gDws85dG7w7HVPm" +
        "WJ5/OZn/eFYiMymLGu18/sf+V31dH8K9SZFj2PBaz/RfT/qfW1mw/9dwCbckPcc2NK69ps4x7H9rv9n/" +
        "pixFlocVfk53Pv8AAAAAAAAAAAAA/scbNV5CDwAoAAA="
)

class TarArchiveReaderTest : FunSpec({
    context("reading") {
        test("reads files with their mode") {
            val entry = read(tarOf("trivy" to "binary".toByteArray())).single()

            entry.name shouldBe "trivy"
            entry.kind shouldBe ArchiveEntryKind.FILE
            entry.bytes.toString(Charsets.UTF_8) shouldBe "binary"
            entry.mode shouldBe 0x1ED
            entry.pathIssue.shouldBeNull()
        }

        test("reads several entries around their padding") {
            val entries = read(tarOf("a" to ByteArray(1), "b" to ByteArray(BLOCK), "c" to ByteArray(BLOCK + 1)))

            entries.map { it.name } shouldBe listOf("a", "b", "c")
            entries.map { it.bytes.size } shouldBe listOf(1, BLOCK, BLOCK + 1)
        }

        test("reads an empty archive") {
            read(terminator()).shouldBeEmpty()
        }

        test("reads a gzip compressed archive, as Trivy releases it") {
            val archive = gzip(tarOf("LICENSE" to "x".toByteArray(), "trivy" to "y".toByteArray()))

            readGzip(archive).map { it.name } shouldBe listOf("LICENSE", "trivy")
        }

        GNU_TAR_ARCHIVES.forEach { (format, base64) ->
            test("reads an archive GNU tar wrote in the $format format") {
                val entries = readGzip(Base64.getDecoder().decode(base64))

                entries.map { it.name } shouldBe listOf("trivy", "contrib/", "contrib/asff.tpl", "link")
                entries.map { it.kind } shouldBe listOf(
                    ArchiveEntryKind.FILE,
                    ArchiveEntryKind.DIRECTORY,
                    ArchiveEntryKind.FILE,
                    ArchiveEntryKind.SYMBOLIC_LINK
                )
                entries.first().bytes.toString(Charsets.UTF_8) shouldBe "binary"
                entries.first().mode shouldBe 0x1ED
                entries.last().linkTarget shouldBe "trivy"
            }
        }

        test("joins the POSIX prefix and name") {
            read(tarBlock("name", prefix = "some/dir") + terminator()).single().name shouldBe "some/dir/name"
        }

        test("ignores the prefix field of old GNU archives") {
            val archive = tarBlock("name", prefix = "not-a-prefix", magic = "ustar ") + terminator()

            read(archive).single().name shouldBe "name"
        }

        mapOf(
            '\u0000' to ArchiveEntryKind.FILE,
            '0' to ArchiveEntryKind.FILE,
            '7' to ArchiveEntryKind.FILE,
            '1' to ArchiveEntryKind.HARD_LINK,
            '2' to ArchiveEntryKind.SYMBOLIC_LINK,
            '3' to ArchiveEntryKind.SPECIAL,
            '4' to ArchiveEntryKind.SPECIAL,
            '5' to ArchiveEntryKind.DIRECTORY,
            '6' to ArchiveEntryKind.SPECIAL
        ).forEach { (type, kind) ->
            test("maps the type ${type.code} to $kind") {
                read(tarBlock("entry", type = type) + terminator()).single().kind shouldBe kind
            }
        }

        test("keeps a link target") {
            val entry = read(tarBlock("link", type = '2', linkName = "target") + terminator()).single()

            entry.linkTarget shouldBe "target"
            entry.pathIssue.shouldBeNull()
        }

        test("reads a size padded with spaces") {
            val block = tarBlock("a", size = 0)
            "         0 \u0000".toByteArray().copyInto(block, 124)
            block.writeChecksum()

            val entry = read(block + terminator()).single()

            entry.bytes.toList().shouldBeEmpty()
        }
    }

    context("traversal") {
        mapOf(
            "../../evil" to ArchivePathIssue.TRAVERSAL,
            "/usr/bin/evil" to ArchivePathIssue.ABSOLUTE,
            "C:\\evil" to ArchivePathIssue.ABSOLUTE
        ).forEach { (name, issue) ->
            test("flags the entry name $name") {
                read(tarOf(name to ByteArray(1))).single().pathIssue shouldBe issue
            }
        }

        test("flags a link that leaves the archive") {
            val entry = read(tarBlock("link", type = '2', linkName = "/etc/passwd") + terminator()).single()

            entry.pathIssue shouldBe ArchivePathIssue.ABSOLUTE
        }

        test("flags a traversal hidden in the prefix") {
            val entry = read(tarBlock("evil", prefix = "../..") + terminator()).single()

            entry.pathIssue shouldBe ArchivePathIssue.TRAVERSAL
        }
    }

    context("limits") {
        test("refuses a gzip bomb without buffering it") {
            val bomb = gzip(tarBlock("bomb", size = 32L * MIB) + ByteArray(32 * MIB) + terminator())
            val budget = ArchiveBudget(ArchiveLimits(maxExtractedBytes = MIB.toLong()))

            shouldThrow<ArchiveException.LimitExceeded> {
                TarArchiveReader(budget).forEachGzipEntry(ByteArrayInputStream(bomb)) { }
            }

            budget.extractedBytes shouldBeLessThan MIB.toLong()
        }

        test("charges the header blocks, so that many empty entries cannot run for free") {
            val headers = (1..100).map { index -> tarBlock("e$index") }
            val archive = headers.fold(ByteArray(0)) { bytes, block -> bytes + block } + terminator()

            shouldThrow<ArchiveException.LimitExceeded> {
                read(archive, ArchiveLimits(maxExtractedBytes = 10L * BLOCK))
            }
        }

        test("refuses too many entries") {
            val archive = tarOf("a" to ByteArray(0), "b" to ByteArray(0))

            shouldThrow<ArchiveException.LimitExceeded> { read(archive, ArchiveLimits(maxEntries = 1)) }
        }

        test("shares its budget with a ZIP reader") {
            val budget = ArchiveBudget(ArchiveLimits(maxExtractedBytes = 3L * BLOCK))
            ZipArchiveReader(budget).readAll(zipOf("a" to ByteArray(2 * BLOCK)))

            shouldThrow<ArchiveException.LimitExceeded> {
                TarArchiveReader(budget).readAll(ByteArrayInputStream(tarOf("b" to ByteArray(BLOCK))))
            }
        }

        test("refuses an entry too large to hold in memory") {
            val archive = tarBlock("big", size = 1L shl 32) + terminator()

            shouldThrow<ArchiveException.LimitExceeded> {
                read(archive, ArchiveLimits(maxExtractedBytes = Long.MAX_VALUE))
            }
        }
    }

    context("malformed archives") {
        test("refuses an archive without an end marker") {
            val archive = tarBlock("a")

            val failure = shouldThrow<ArchiveException.Malformed> { read(archive) }

            failure.message.orEmpty() shouldContain "truncated"
        }

        test("refuses a lone end block followed by data") {
            val archive = ByteArray(BLOCK) + tarBlock("hidden")

            shouldThrow<ArchiveException.Malformed> { read(archive) }
        }

        test("refuses truncated content") {
            val archive = tarBlock("a", size = 4096) + ByteArray(BLOCK)

            shouldThrow<ArchiveException.Malformed> { read(archive) }
        }

        test("refuses a partial header") {
            shouldThrow<ArchiveException.Malformed> { read(ByteArray(100)) }
        }

        test("refuses a wrong checksum") {
            val block = tarBlock("a")
            block[0] = 'b'.code.toByte()

            val failure = shouldThrow<ArchiveException.Malformed> { read(block + terminator()) }

            failure.message.orEmpty() shouldContain "checksum"
        }

        test("refuses a garbage size") {
            val block = tarBlock("a")
            "zzzzzzzzzzz".toByteArray().copyInto(block, 124)
            block.writeChecksum()

            shouldThrow<ArchiveException.Malformed> { read(block + terminator()) }
        }

        test("refuses a base-256 size") {
            val block = tarBlock("a")
            block[124] = 0x80.toByte()
            block.writeChecksum()

            shouldThrow<ArchiveException.Malformed> { read(block + terminator()) }
        }

        listOf('x', 'g', 'L', 'K', 'S').forEach { type ->
            test("fails closed on the extension type $type, which could change the entry name") {
                val failure = shouldThrow<ArchiveException.Malformed> {
                    read(tarBlock("a", type = type) + terminator())
                }

                failure.message.orEmpty() shouldContain "not supported"
            }
        }

        test("refuses a directory that declares content") {
            shouldThrow<ArchiveException.Malformed> { read(tarBlock("a", size = 4, type = '5') + ByteArray(BLOCK)) }
        }

        test("refuses bytes that are not gzip") {
            val failure = shouldThrow<ArchiveException.Malformed> { readGzip("not gzip at all".toByteArray()) }

            failure.message.orEmpty() shouldContain "gzip"
        }

        test("refuses a truncated gzip stream") {
            val archive = gzip(tarOf("a" to ByteArray(4096) { index -> index.toByte() }))

            shouldThrow<ArchiveException.Malformed> { readGzip(archive.copyOf(archive.size / 2)) }
        }
    }

    test("hands every entry to the action in archive order") {
        val names = mutableListOf<String>()

        TarArchiveReader(ArchiveBudget()).forEachEntry(ByteArrayInputStream(tarOf("a" to ByteArray(0)))) { entry ->
            names.add(entry.name)
        }

        names shouldHaveSize 1
    }
})
