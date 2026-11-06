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
package com.davils.kreate.advisory.osv

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

private val UNSAFE_NAME_CHARACTERS: Regex = Regex("[^A-Za-z0-9._-]")

internal class OsvCache(private val directory: Path) {
    fun read(id: String): String? {
        val file = fileOf(id)
        if (!Files.isRegularFile(file)) return null
        return Files.readString(file)
    }

    fun write(id: String, record: String) {
        try {
            Files.createDirectories(directory)
            val temporary = Files.createTempFile(directory, ".record", ".tmp")
            Files.writeString(temporary, record)
            Files.move(temporary, fileOf(id), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: IOException) {
            return
        }
    }

    private fun fileOf(id: String): Path {
        val name = id.replace(UNSAFE_NAME_CHARACTERS, "_")
        return directory.resolve("$name.json")
    }
}
