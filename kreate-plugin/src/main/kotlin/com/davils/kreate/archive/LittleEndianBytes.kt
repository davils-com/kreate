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

package com.davils.kreate.archive

private const val BYTE_MASK: Int = 0xFF

private const val BYTE_BITS: Int = 8

private const val SHORT_BITS: Int = 16

internal class LittleEndianBytes(private val bytes: ByteArray) {
    val size: Int
        get() = bytes.size

    fun uint8(offset: Long): Int {
        if (offset !in bytes.indices) {
            throw ArchiveException.Malformed("The archive ends inside a header.")
        }
        return bytes[offset.toInt()].toInt() and BYTE_MASK
    }

    fun uint16(offset: Long): Int = uint8(offset) or (uint8(offset + 1) shl BYTE_BITS)

    fun uint32(offset: Long): Long {
        val low = uint16(offset).toLong()
        val high = uint16(offset + 2).toLong()
        return low or (high shl SHORT_BITS)
    }

    fun slice(offset: Long, length: Long): ByteArray {
        val end = offset + length
        if (offset < 0 || end > bytes.size) {
            throw ArchiveException.Malformed("An archive entry lies outside the archive.")
        }
        return bytes.copyOfRange(offset.toInt(), end.toInt())
    }
}
