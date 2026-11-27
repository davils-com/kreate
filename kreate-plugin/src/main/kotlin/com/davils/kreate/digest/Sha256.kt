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

package com.davils.kreate.digest

import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.HexFormat

private const val ALGORITHM: String = "SHA-256"

internal object Sha256 {
    fun digest(): MessageDigest = MessageDigest.getInstance(ALGORITHM)

    fun digestOf(bytes: ByteArray): ByteArray = digest().digest(bytes)

    fun digestOf(text: String): ByteArray = digestOf(text.toByteArray(Charsets.UTF_8))

    fun hexOf(bytes: ByteArray): String = HexFormat.of().formatHex(digestOf(bytes))

    fun hexOf(text: String): String = HexFormat.of().formatHex(digestOf(text))

    fun hexOf(file: Path): String = Files.newInputStream(file).use(::hexOf)

    fun hexOf(stream: InputStream): String {
        val digest = digest()
        DigestInputStream(stream, digest).transferTo(OutputStream.nullOutputStream())
        return HexFormat.of().formatHex(digest.digest())
    }
}
