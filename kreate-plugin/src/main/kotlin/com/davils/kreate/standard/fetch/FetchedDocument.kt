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
package com.davils.kreate.standard.fetch

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

internal class FetchedDocument(val path: Path, val fetchedAt: Instant) {
    fun bytes(): ByteArray = Files.readAllBytes(path)

    fun text(): String {
        val decoded = decoder().decode(ByteBuffer.wrap(bytes()))
        return decoded.toString()
    }

    fun reader(): BufferedReader {
        val stream = Files.newInputStream(path)
        return BufferedReader(InputStreamReader(stream, decoder()))
    }

    private fun decoder(): CharsetDecoder {
        val decoder = StandardCharsets.UTF_8.newDecoder()
        decoder.onMalformedInput(CodingErrorAction.REPLACE)
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE)
        return decoder
    }
}
