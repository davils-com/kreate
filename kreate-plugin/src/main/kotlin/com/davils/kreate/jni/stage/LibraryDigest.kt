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

package com.davils.kreate.jni.stage

import java.io.File
import java.security.MessageDigest

private const val DIGEST_ALGORITHM: String = "SHA-256"

private const val READ_BUFFER: Int = 64 * 1024

private const val HEXADECIMAL: String = "0123456789abcdef"

private const val NIBBLE: Int = 4

private const val LOW_NIBBLE: Int = 0xF

private const val BYTE_MASK: Int = 0xFF

internal object LibraryDigest {
    fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance(DIGEST_ALGORITHM)
        file.inputStream().use { source ->
            val buffer = ByteArray(READ_BUFFER)
            var read = source.read(buffer)
            while (read >= 0) {
                digest.update(buffer, 0, read)
                read = source.read(buffer)
            }
        }

        return toHexadecimal(digest.digest())
    }

    private fun toHexadecimal(digest: ByteArray): String {
        val rendered = StringBuilder(digest.size * 2)
        for (byte in digest) {
            val value = byte.toInt() and BYTE_MASK
            rendered.append(HEXADECIMAL[value ushr NIBBLE])
            rendered.append(HEXADECIMAL[value and LOW_NIBBLE])
        }

        return rendered.toString()
    }
}
