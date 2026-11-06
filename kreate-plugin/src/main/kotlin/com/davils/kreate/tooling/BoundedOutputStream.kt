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

package com.davils.kreate.tooling

import java.io.ByteArrayOutputStream
import java.io.OutputStream

internal class BoundedOutputStream(private val maxBytes: Int) : OutputStream() {
    private val retained: ByteArrayOutputStream = ByteArrayOutputStream()

    private var isTruncated: Boolean = false

    @Synchronized
    override fun write(byte: Int) {
        if (retained.size() >= maxBytes) {
            isTruncated = true
            return
        }
        retained.write(byte)
    }

    @Synchronized
    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        val room = maxBytes - retained.size()
        val kept = minOf(room, length)
        if (kept < length) isTruncated = true
        retained.write(bytes, offset, kept)
    }

    @Synchronized
    fun snapshot(): ToolOutput = ToolOutput(retained.toByteArray(), isTruncated)
}
