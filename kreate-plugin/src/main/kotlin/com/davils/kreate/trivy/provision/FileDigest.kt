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
package com.davils.kreate.trivy.provision

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat

private const val DIGEST_ALGORITHM: String = "SHA-256"

private const val BUFFER_BYTES: Int = 64 * 1024

internal fun sha256Of(file: Path): String {
    val digest = MessageDigest.getInstance(DIGEST_ALGORITHM)
    Files.newInputStream(file).use { input ->
        val buffer = ByteArray(BUFFER_BYTES)
        var read = input.read(buffer)
        while (read >= 0) {
            digest.update(buffer, 0, read)
            read = input.read(buffer)
        }
    }
    return HexFormat.of().formatHex(digest.digest())
}
