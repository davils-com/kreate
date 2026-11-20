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
import com.davils.kreate.archive.gzip
import com.davils.kreate.archive.tarOf
import com.davils.kreate.archive.zipOf
import com.davils.kreate.freshDirectory
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardIds
import com.davils.kreate.standard.model.SuiteSource
import com.davils.kreate.standard.provider.ProviderFixture
import com.davils.kreate.standard.provider.failure
import com.davils.kreate.standard.provider.snapshotFor
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import java.security.MessageDigest
import java.util.HexFormat

private const val VENDORED: String = "src/test/resources/xmlconf"

private const val ARCHIVE_PATH: String = "/XML/Test/xmlts.tar.gz"

private const val COMMIT: String = "7bc3365c652a322f3d762021b3879eb92dae7e30"

private const val HEAD: String = "0123456789abcdef0123456789abcdef01234567"

private const val HELLO_BLOB: String = "ce013625030ba8dba906f756967f9e9ca394464a"

private val FILES: List<Pair<String, ByteArray>> = listOf(
    "a.xml" to "<a/>\n".toByteArray(),
    "sub/b.xml" to "<b>text</b>\n".toByteArray()
)

private fun sha256Of(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return HexFormat.of().formatHex(digest)
}

private fun archiveOf(vararg extra: Pair<String, ByteArray>): ByteArray {
    val vendored = FILES.map { (path, bytes) -> "xmlconf/$path" to bytes }
    val entries = vendored + extra
    return gzip(tarOf(*entries.toTypedArray()))
}

private fun vendor(project: File, files: List<Pair<String, ByteArray>> = FILES): File {
    val directory = File(project, VENDORED)
    files.forEach { (path, bytes) ->
        val file = File(directory, path)
        file.parentFile.mkdirs()
        file.writeBytes(bytes)
    }
    return directory
}

class SuiteProviderTest : FunSpec({
    lateinit var project: File
    lateinit var fixture: ProviderFixture

    beforeEach {
        val workspace = tempdir()
        project = workspace.freshDirectory("project")
        fixture = ProviderFixture(workspace.freshDirectory("cache"), project = project)
    }

    afterEach { fixture.close() }

    fun archived(archive: ByteArray, root: String? = null): StandardDeclaration {
        fixture.bytes(ARCHIVE_PATH, archive)
        val url = fixture.server.uri(ARCHIVE_PATH).toString()
        val source = SuiteSource(vendored = VENDORED, archive = url, sha256 = sha256Of(archive), root = root)
        return StandardDeclaration(StandardIds.suite("xmlconf"), suite = source)
    }

    fun mismatchOf(declaration: StandardDeclaration): String {
        val outcome = SuiteProvider(fixture.context).snapshotOf(declaration)
        val failure = outcome.failure()
        return failure.shouldBeInstanceOf<LookupFailure.Mismatch>().reason
    }

    context("from an archive") {
        test("records the digest of the vendored files, the pinned sha256 and the watched page") {
            vendor(project)
            fixture.text("/XML/Test/", "<h1>XML Test Suite</h1>")
            val archive = archiveOf("xmlconf/unvendored.xml" to "<c/>".toByteArray())
            val declared = archived(archive)
            val watch = fixture.server.uri("/XML/Test/").toString()
            val watched = declared.copy(suite = declared.suite?.copy(watch = watch))

            val snapshot = SuiteProvider(fixture.context).snapshotFor(watched)

            snapshot.title shouldBe "xmlts.tar.gz"
            snapshot.revision shouldBe "sha256:${sha256Of(archive)}"
            snapshot.contentDigest.orEmpty() shouldStartWith "sha256:"
            snapshot.fingerprint.orEmpty() shouldStartWith "sha256:"
        }

        test("refuses an archive that no longer has the pinned sha256") {
            vendor(project)
            val declared = archived(archiveOf())
            val repinned = declared.copy(suite = declared.suite?.copy(sha256 = "0".repeat(64)))

            mismatchOf(repinned) shouldContain "not the pinned ${"0".repeat(64)}"
        }

        test("refuses a vendored file whose line endings a checkout converted") {
            vendor(project, listOf("a.xml" to "<a/>\r\n".toByteArray(), FILES[1]))

            val archive = fixture.server.uri(ARCHIVE_PATH)

            mismatchOf(archived(archiveOf())) shouldBe "The vendored file a.xml differs from the archive $archive."
        }

        test("refuses vendored files the archive does not hold") {
            vendor(project, FILES + ("x.xml" to "x".toByteArray()) + ("y.xml" to "y".toByteArray()))

            mismatchOf(archived(archiveOf())) shouldContain "2 vendored files are not in the archive"
        }

        test("refuses an archive with a path that leaves its directory") {
            vendor(project)

            mismatchOf(archived(archiveOf("../evil.xml" to "x".toByteArray()))) shouldContain
                "holds the unsafe path ../evil.xml"
        }

        test("refuses a vendored directory that does not exist") {
            mismatchOf(archived(archiveOf())) shouldBe "The vendored directory $VENDORED does not exist."
        }

        test("compares a zip archive from its root when told so") {
            vendor(project)
            val zip = zipOf(*FILES.toTypedArray())

            val snapshot = SuiteProvider(fixture.context).snapshotFor(archived(zip, root = ""))

            snapshot.contentDigest.orEmpty() shouldStartWith "sha256:"
        }
    }

    context("from a GitHub repository") {
        fun repository(blob: String, truncated: Boolean = false): StandardDeclaration {
            val tree = """
                {"sha": "$COMMIT", "truncated": $truncated, "tree": [
                {"path": "tests", "type": "tree", "sha": "x"},
                {"path": "tests/hello.txt", "type": "blob", "sha": "$blob"}]}
            """.trimIndent()
            fixture.json("/repos/w3c/xsdtests/git/trees/$COMMIT", tree)
            val head = """[{"sha": "$HEAD", "commit": {"committer": {"date": "2026-04-01T12:01:05Z"}}}]"""
            fixture.json("/repos/w3c/xsdtests/commits", head)
            vendor(project, listOf("hello.txt" to "hello\n".toByteArray()))
            val source = SuiteSource(vendored = VENDORED, github = "w3c/xsdtests", commit = COMMIT, root = "tests")
            return StandardDeclaration(StandardIds.suite("xsdtests"), suite = source)
        }

        test("compares the vendored files with the blobs of the pinned commit and records the head") {
            val snapshot = SuiteProvider(fixture.context).snapshotFor(repository(HELLO_BLOB))

            snapshot.title shouldBe "w3c/xsdtests"
            snapshot.revision shouldBe HEAD
            snapshot.published shouldBe "2026-04-01"
            snapshot.contentDigest.orEmpty() shouldStartWith "sha256:"
        }

        test("refuses a vendored file that differs from the blob of the pinned commit") {
            mismatchOf(repository("0".repeat(40))) shouldContain "differs from w3c/xsdtests at $COMMIT"
        }

        test("is unavailable while GitHub truncates the tree") {
            val outcome = SuiteProvider(fixture.context).snapshotOf(repository(HELLO_BLOB, truncated = true))

            outcome.failure().shouldBeInstanceOf<LookupFailure.Unavailable>().reason shouldContain "truncates"
        }
    }

    context("the digest of the vendored files") {
        fun digestOf(files: List<Pair<String, ByteArray>>): String {
            val directory = vendor(tempdir().freshDirectory(), files)
            val tree = VendoredTree.read(directory.toPath(), VENDORED)
            return tree.shouldBeInstanceOf<Outcome.Success<VendoredTree>>().value.digest
        }

        test("depends on the paths and the bytes of the files, not on when they were written") {
            val digest = digestOf(FILES)

            digestOf(FILES) shouldBe digest
            digestOf(listOf("a.xml" to FILES[0].second, "sub/c.xml" to FILES[1].second)) shouldNotBe digest
            digestOf(listOf("a.xml" to "<a/>\r\n".toByteArray(), FILES[1])) shouldNotBe digest
            digestOf(FILES + ("empty.xml" to ByteArray(0))) shouldNotBe digest
        }
    }
})
