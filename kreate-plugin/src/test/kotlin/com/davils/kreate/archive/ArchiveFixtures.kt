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

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val TAR_BLOCK: Int = 512

private const val CENTRAL_SIGNATURE: Int = 0x02014B50

private val BYTE_ORDER: ByteOrder = ByteOrder.LITTLE_ENDIAN

internal fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
    val bytes = ByteArrayOutputStream()
    ZipOutputStream(bytes).use { zip ->
        entries.forEach { (name, content) ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(content)
            zip.closeEntry()
        }
    }
    return bytes.toByteArray()
}

internal fun storedZipOf(name: String, content: ByteArray): ByteArray {
    val checksum = CRC32()
    checksum.update(content)
    val entry = ZipEntry(name)
    entry.method = ZipEntry.STORED
    entry.size = content.size.toLong()
    entry.compressedSize = content.size.toLong()
    entry.crc = checksum.value

    val bytes = ByteArrayOutputStream()
    ZipOutputStream(bytes).use { zip ->
        zip.putNextEntry(entry)
        zip.write(content)
        zip.closeEntry()
    }
    return bytes.toByteArray()
}

internal fun ByteArray.centralHeader(index: Int = 0): Int {
    val buffer = ByteBuffer.wrap(this).order(BYTE_ORDER)
    val headers = (0..size - Int.SIZE_BYTES).filter { offset -> buffer.getInt(offset) == CENTRAL_SIGNATURE }
    return headers[index]
}

internal fun ByteArray.endRecord(): Int = lastIndexOfSignature(0x06054B50)

internal fun ByteArray.lastIndexOfSignature(signature: Int): Int {
    val buffer = ByteBuffer.wrap(this).order(BYTE_ORDER)
    return (0..size - Int.SIZE_BYTES).last { offset -> buffer.getInt(offset) == signature }
}

internal fun ByteArray.withInt(offset: Int, value: Int): ByteArray {
    val copy = copyOf()
    val buffer = ByteBuffer.wrap(copy).order(BYTE_ORDER)
    buffer.putInt(offset, value)
    return copy
}

internal fun ByteArray.withShort(offset: Int, value: Int): ByteArray {
    val copy = copyOf()
    val buffer = ByteBuffer.wrap(copy).order(BYTE_ORDER)
    buffer.putShort(offset, value.toShort())
    return copy
}

internal fun ByteArray.withByte(offset: Int, value: Int): ByteArray {
    val copy = copyOf()
    copy[offset] = value.toByte()
    return copy
}

internal fun tarBlock(
    name: String,
    size: Long = 0,
    type: Char = '0',
    linkName: String = "",
    prefix: String = "",
    magic: String = "ustar\u0000"
): ByteArray {
    val block = ByteArray(TAR_BLOCK)
    block.put(0, name)
    block.put(100, "0000755\u0000")
    block.put(124, size.toString(8).padStart(11, '0') + "\u0000")
    block.put(136, "00000000000\u0000")
    block[156] = type.code.toByte()
    block.put(157, linkName)
    block.put(257, magic)
    block.put(263, "00")
    block.put(345, prefix)
    block.writeChecksum()
    return block
}

internal fun ByteArray.writeChecksum() {
    fill(' '.code.toByte(), 148, 156)
    val sum = sumOf { byte -> byte.toInt() and 0xFF }
    put(148, sum.toString(8).padStart(6, '0') + "\u0000 ")
}

private fun ByteArray.put(offset: Int, text: String) {
    val encoded = text.toByteArray()
    encoded.copyInto(this, offset)
}

internal fun tarContent(content: ByteArray): ByteArray {
    val padding = (TAR_BLOCK - content.size % TAR_BLOCK) % TAR_BLOCK
    return content + ByteArray(padding)
}

internal fun tarOf(vararg entries: Pair<String, ByteArray>): ByteArray {
    val blocks = entries.map { (name, content) -> tarBlock(name, content.size.toLong()) + tarContent(content) }
    return blocks.fold(ByteArray(0)) { archive, block -> archive + block } + ByteArray(2 * TAR_BLOCK)
}

internal fun gzip(content: ByteArray): ByteArray {
    val bytes = ByteArrayOutputStream()
    GZIPOutputStream(bytes).use { stream -> stream.write(content) }
    return bytes.toByteArray()
}
