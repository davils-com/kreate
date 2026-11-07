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
package com.davils.kreate.provenance.depsdev

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.FileTime
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.util.HexFormat

private const val KEY_ALGORITHM: String = "SHA-256"

internal class DepsDevCache(private val directory: Path, private val timeToLive: Duration, private val clock: Clock) {
    fun read(key: String): String? {
        val file = fileOf(key)
        if (!Files.isRegularFile(file)) return null
        val written = Files.getLastModifiedTime(file).toInstant()
        val age = Duration.between(written, clock.instant())
        if (age > timeToLive || age.isNegative) return null
        return Files.readString(file)
    }

    fun write(key: String, text: String) {
        try {
            Files.createDirectories(directory)
            val temporary = Files.createTempFile(directory, ".answer", ".tmp")
            Files.writeString(temporary, text)
            Files.setLastModifiedTime(temporary, FileTime.from(clock.instant()))
            Files.move(temporary, fileOf(key), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: IOException) {
            return
        }
    }

    private fun fileOf(key: String): Path {
        val digest = MessageDigest.getInstance(KEY_ALGORITHM).digest(key.toByteArray())
        return directory.resolve(HexFormat.of().formatHex(digest) + ".json")
    }
}
