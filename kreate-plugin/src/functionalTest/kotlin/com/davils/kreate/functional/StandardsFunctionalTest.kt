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
package com.davils.kreate.functional

import com.sun.net.httpserver.HttpServer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.HexFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val HTTP_OK: Int = 200

private const val HTTP_UNAVAILABLE: Int = 503

private const val REVIEWED_LOCK: String = """
{
  "schemaVersion": 1,
  "standards": [
    {
      "id": "document:ecma-404",
      "reviewedOn": "2026-11-01",
      "snapshot": { "title": "ECMA-404" }
    }
  ]
}
"""

private val SUITE_FILES: Map<String, String> = mapOf(
    "a.xml" to "<a/>\n",
    "b.xml" to "<b/>\n",
    "refs.txt" to "The suite cites RFC 7396.\n"
)

private fun suiteArchive(): ByteArray {
    val bytes = ByteArrayOutputStream()
    ZipOutputStream(bytes).use { zip ->
        SUITE_FILES.forEach { (path, text) ->
            zip.putNextEntry(ZipEntry("xmlconf/$path"))
            zip.write(text.toByteArray())
            zip.closeEntry()
        }
    }
    return bytes.toByteArray()
}

private fun sha256Of(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return HexFormat.of().formatHex(digest)
}

private fun specification(shortname: String): String = """
    {"shortname": "$shortname", "title": "XML $shortname", "_links": {
      "latest-version": {
        "href": "https://api.w3.org/specifications/$shortname/versions/20081126", "title": "Recommendation"},
      "series": {"href": "https://api.w3.org/specification-series/xml"}}}
""".trimIndent()

private fun version(shortname: String): String =
    """{"status": "Recommendation", "errata": "https://www.w3.org/XML/$shortname-errata"}"""

private const val XML_SERIES: String =
    """{"_links": {"current-specification": {"href": "https://api.w3.org/specifications/xml11"}}}"""

private const val ERRATA_PAGE: String = """<html><body><h3><a name="S01">S01</a></h3></body></html>"""

class StandardsFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()
    val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
    val pages = listOf("/ecma-404" to "The JSON syntax.", "/ecma-404-2" to "The JSON syntax, second edition.")
    pages.forEach { (path, text) ->
        server.createContext(path) { exchange ->
            val body = text.toByteArray()
            exchange.sendResponseHeaders(HTTP_OK, body.size.toLong())
            exchange.responseBody.use { stream -> stream.write(body) }
        }
    }
    val archive = suiteArchive()
    val routes = mapOf(
        "/suite/xmlconf.zip" to archive,
        "/suite/" to "<h1>XML Test Suite</h1>".toByteArray(),
        "/api/specifications/xml" to specification("xml").toByteArray(),
        "/api/specifications/xml11" to specification("xml11").toByteArray(),
        "/api/specifications/xml/versions/20081126" to version("xml").toByteArray(),
        "/api/specifications/xml11/versions/20081126" to version("xml11").toByteArray(),
        "/api/specification-series/xml" to XML_SERIES.toByteArray(),
        "/w3c/XML/xml-errata" to ERRATA_PAGE.toByteArray(),
        "/w3c/XML/xml11-errata" to ERRATA_PAGE.toByteArray()
    )
    routes.forEach { (path, body) ->
        server.createContext(path) { exchange ->
            exchange.sendResponseHeaders(HTTP_OK, body.size.toLong())
            exchange.responseBody.use { stream -> stream.write(body) }
        }
    }
    server.createContext("/down") { exchange ->
        exchange.sendResponseHeaders(HTTP_UNAVAILABLE, -1)
        exchange.close()
    }
    server.start()
    afterSpec { server.stop(0) }

    val base = "http://${server.address.address.hostAddress}:${server.address.port}"

    val w3c = arrayOf("-Pkreate.network.w3cApiUrl=$base/api", "-Pkreate.network.w3cUrl=$base/w3c")

    val suite = """
        suite("xmlconf") {
            archive = "$base/suite/xmlconf.zip"
            sha256 = "${sha256Of(archive)}"
            vendored = "src/test/resources/xmlconf"
            watch = "$base/suite/"
        }
    """.trimIndent()

    fun watched(path: String): String =
        """document("ECMA-404") { reviewEvery = Duration.ofDays(3650); url = "$base$path" }"""

    fun newBuild(standardsBlock: String, assuranceBlock: String = ""): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.writeBuild(
            kreateBlock = """
                ${KreateBuildFixture.platformBlock}

                assurance {
                    enabled = true
                    $assuranceBlock
                    standards {
                        enabled = true
                        $standardsBlock
                    }
                }
            """.trimIndent()
        )
        val script = fixture.file("build.gradle.kts").readText()
        fixture.write("build.gradle.kts", "import java.time.Duration\n\n$script")
        return fixture
    }

    context("the standards") {
        test("are not checked unless they are enabled") {
            val fixture = KreateBuildFixture.createIn(workspace)
            fixture.writeSettings()
            fixture.writeBuild("""${KreateBuildFixture.platformBlock}

                assurance { enabled = true }
            """.trimIndent())

            fixture.build("tasks", "--all").output shouldNotContain "kreateStandards"
        }

        test("refuse a declaration nothing can check without an interval") {
            val fixture = newBuild("""din("DIN 5008")""")

            val result = fixture.buildAndFail("kreateStandardsInventory")

            result.output shouldContain "din:5008 needs reviewEvery"
        }

        test("block the gate while a declared standard was never reviewed") {
            val fixture = newBuild("""document("ECMA-404") { reviewEvery = Duration.ofDays(365) }""")

            val result = fixture.buildAndFail("kreateAssurance")

            result.output shouldContain "STANDARD_UNREVIEWED: document:ecma-404 is declared but was never reviewed"
            result.output shouldContain "Kreate assurance [FINDINGS]"
        }

        test("pass once standards.lock records the review") {
            val fixture = newBuild("""document("ECMA-404") { reviewEvery = Duration.ofDays(36500) }""")
            fixture.write("standards.lock", REVIEWED_LOCK.trimStart())

            val result = fixture.build("kreateStandardsInventory")

            result.output shouldContain "Checked 1 standard(s): 0 finding(s)."
        }

        test("name a standard the code refers to without declaring it, with its file and line") {
            val fixture = newBuild("")
            fixture.writeKotlin("Merge.kt", "package sample\n\nval specification = \"RFC 7396\"\n")

            val result = fixture.build("kreateStandardsInventory")

            result.output shouldContain "MEDIUM STANDARD_UNDECLARED: src/main/kotlin/Merge.kt names rfc:7396"
            fixture.file("build/kreate/assurance/documents/standards-inventory.json").readText() shouldContain
                "\"line\": 3"
        }

        test("let an ignore rule accept a standard the code only mentions") {
            val ignore = """ignore("STANDARD_UNDECLARED") { reason = "Only mentioned."; module = "rfc:7396" }"""
            val fixture = newBuild("", ignore)
            fixture.writeKotlin("Merge.kt", "package sample\n\nval specification = \"RFC 7396\"\n")

            val result = fixture.build("kreateAssurance")

            result.output shouldContain "Kreate assurance [PASSED]"
        }

        test("record a review that lets the gate pass, and report a changed watched page") {
            val fixture = newBuild(watched("/ecma-404"))

            val review = fixture.build("kreateStandardsReview", "--all", "--note=Read the first edition.")

            review.output shouldContain "Recorded 1 review(s)"
            fixture.file("standards.lock").readText() shouldContain "sha256:"
            fixture.build("kreateAssurance").output shouldContain "Kreate assurance [PASSED]"

            val script = fixture.file("build.gradle.kts").readText()
            fixture.write("build.gradle.kts", script.replace("/ecma-404\"", "/ecma-404-2\""))
            val changed = fixture.build("kreateAssurance")

            changed.output shouldContain "MEDIUM STANDARD_CHANGED: The watched page of document:ecma-404 changed"
        }

        test("need a note and a known standard to record a review") {
            val fixture = newBuild(watched("/ecma-404"))

            fixture.buildAndFail("kreateStandardsReview", "--all").output shouldContain "--note"
            val misspelt = fixture.buildAndFail("kreateStandardsReview", "--standard=document:ecma-40", "--note=x")

            misspelt.output shouldContain "Did you mean document:ecma-404?"
        }

        test("refuse to review a standard whose page cannot be read") {
            val fixture = newBuild(watched("/down"))

            val result = fixture.buildAndFail("kreateStandardsReview", "--all", "--note=Read it.")

            result.output shouldContain "Cannot review document:ecma-404"
        }

        test("are incomplete in offline mode") {
            val fixture = newBuild(watched("/ecma-404"))

            val result = fixture.buildAndFail("kreateAssurance", "--offline")

            result.output shouldContain "Standards cannot be checked in offline mode."
            result.output shouldContain "Kreate assurance [INCOMPLETE]"
        }

        test("are explained by their id") {
            val fixture = newBuild("rfc(9562)")

            val standard = fixture.build("kreateExplain", "--rule=RFC:9562")
            val rule = fixture.build("kreateExplain", "--rule=standard_obsoleted")

            standard.output shouldContain "rfc:9562 is a standard of the kind rfc."
            standard.output shouldContain "https://www.rfc-editor.org/info/rfc9562"
            standard.output shouldContain "kreateStandardsReview --standard=rfc:9562"
            rule.output shouldContain "STANDARD_OBSOLETED (HIGH, standards/lifecycle)"
        }

        test("are cached while nothing they read changes") {
            val fixture = newBuild("")

            fixture.build("kreateStandardsInventory", "kreateStandardsAudit")
            val second = fixture.build("kreateStandardsInventory", "kreateStandardsAudit")

            second.task(":kreateStandardsInventory")?.outcome?.name shouldBe "UP_TO_DATE"
            second.task(":kreateStandardsAudit")?.outcome?.name shouldBe "UP_TO_DATE"
        }

        test("check a vendored suite against its pinned archive and report what changed since the review") {
            val fixture = newBuild(suite)
            listOf("a.xml", "refs.txt").forEach { path ->
                fixture.write("src/test/resources/xmlconf/$path", SUITE_FILES.getValue(path))
            }

            fixture.build("kreateStandardsReview", "--all", "--note=Vendored xmlts.")

            fixture.file("standards.lock").readText() shouldContain "\"contentDigest\": \"sha256:"
            fixture.build("kreateAssurance").output shouldContain "Kreate assurance [PASSED]"

            fixture.file("src/test/resources/xmlconf/a.xml").writeText("<a/>\r\n")
            val converted = fixture.buildAndFail("kreateAssurance")

            converted.output shouldContain
                "HIGH STANDARD_SUITE_MISMATCH: The vendored copy of suite:xmlconf does not match what it pins"

            fixture.write("src/test/resources/xmlconf/a.xml", SUITE_FILES.getValue("a.xml"))
            fixture.write("src/test/resources/xmlconf/b.xml", SUITE_FILES.getValue("b.xml"))
            val added = fixture.buildAndFail("kreateAssurance")

            added.output shouldContain
                "HIGH STANDARD_SUITE_MODIFIED: The vendored copy of suite:xmlconf changed since its review"
            added.output shouldNotContain "STANDARD_UNDECLARED"
        }

        test("read the errata of W3C specifications and take a declared newer level for no update") {
            val fixture = newBuild("""w3c("xml"); w3c("xml11")""")
            fixture.write("docs/xml.md", "See https://www.w3.org/TR/REC-xml/ and https://www.w3.org/TR/xpath.\n")

            fixture.build("kreateStandardsReview", "--all", "--note=Read XML.", *w3c)

            val lock = fixture.file("standards.lock").readText()
            lock shouldContain "\"S01\": \"VERIFIED\""
            lock shouldNotContain "\"w3c:xml11\"\n"
            val gate = fixture.build("kreateAssurance", *w3c)

            gate.output shouldContain "MEDIUM STANDARD_VERSIONLESS: docs/xml.md links w3c:xpath, which names no version"
            gate.output shouldNotContain "STANDARD_UNDECLARED"
            gate.output shouldContain "Kreate assurance [PASSED]"
        }
    }
})
