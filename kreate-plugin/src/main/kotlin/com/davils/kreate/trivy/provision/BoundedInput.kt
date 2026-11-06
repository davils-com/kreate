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

import java.io.InputStream

internal class BoundedInput(private val input: InputStream, private var remaining: Long) : InputStream() {
    override fun read(): Int {
        if (remaining <= 0L) return END_OF_STREAM
        val value = input.read()
        if (value != END_OF_STREAM) remaining -= 1
        return value
    }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
        if (remaining <= 0L) return END_OF_STREAM
        val wanted = minOf(length.toLong(), remaining).toInt()
        val read = input.read(bytes, offset, wanted)
        if (read > 0) remaining -= read
        return read
    }

    private companion object {
        private const val END_OF_STREAM: Int = -1
    }
}
