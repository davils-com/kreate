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

package com.davils.kreate.net

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files

class ExtraCertificateAuthorityTest : FunSpec({
    test("builds a context from a PEM file") {
        ExtraCertificateAuthority.sslContext(LoopbackCertificate.pemFile).shouldNotBeNull()
    }

    test("names a missing file") {
        val missing = LoopbackCertificate.pemFile.resolveSibling("missing.pem")

        val failure = shouldThrow<NetworkException.Misconfigured> { ExtraCertificateAuthority.sslContext(missing) }

        failure.message.orEmpty() shouldContain "missing.pem"
    }

    test("refuses a file without a certificate") {
        val empty = Files.createTempFile("kreate-empty", ".pem")

        shouldThrow<NetworkException.Misconfigured> { ExtraCertificateAuthority.sslContext(empty) }
    }

    test("refuses a corrupt certificate") {
        val corrupt = Files.createTempFile("kreate-corrupt", ".pem")
        Files.writeString(corrupt, "-----BEGIN CERTIFICATE-----\nnot base64 !!\n-----END CERTIFICATE-----\n")

        shouldThrow<NetworkException.Misconfigured> { ExtraCertificateAuthority.sslContext(corrupt) }
    }
})
