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
package com.davils.kreate.provenance.confusion

import com.davils.kreate.net.ScriptedTransport
import com.davils.kreate.net.responseOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val BASE: String = "https://repo.example/maven2"

private fun answering(status: Int, body: String = ""): ScriptedTransport =
    ScriptedTransport { request -> responseOf(status, request.uri.toString(), body = body.toByteArray()) }

class MavenMetadataClientTest : FunSpec({
    context("the public repository") {
        test("is asked for the metadata of the module") {
            val transport = answering(200, "<metadata><versioning><release>2.0</release></versioning></metadata>")

            MavenMetadataClient(transport, BASE).latestVersionOf("com.acme.secret", "billing") shouldBe "2.0"

            transport.requests.single().uri.path shouldBe "/maven2/com/acme/secret/billing/maven-metadata.xml"
        }

        test("names the last version when no release is declared") {
            val versions = "<versions><version>1</version><version>3</version></versions>"
            val metadata = "<metadata><versioning>$versions</versioning></metadata>"

            MavenMetadataClient(answering(200, metadata), BASE).latestVersionOf("a", "b") shouldBe "3"
        }

        test("answers nothing for a module it does not publish") {
            val client = MavenMetadataClient(answering(404), BASE)

            client.latestVersionOf("a", "b").shouldBeNull()
        }

        test("is unavailable when it fails") {
            shouldThrow<PublicRepositoryUnavailableException> {
                MavenMetadataClient(answering(502), BASE).latestVersionOf("a", "b")
            }
        }

        test("does not resolve an external entity") {
            val metadata = """<!DOCTYPE m [<!ENTITY e SYSTEM "file:///etc/passwd">]><metadata>&e;</metadata>"""

            shouldThrow<PublicRepositoryUnavailableException> {
                MavenMetadataClient(answering(200, metadata), BASE).latestVersionOf("a", "b")
            }
        }
    }
})
