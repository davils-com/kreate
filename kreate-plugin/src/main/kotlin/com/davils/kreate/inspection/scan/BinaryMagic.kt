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
package com.davils.kreate.inspection.scan

private val ELF_MAGIC: ByteArray = byteArrayOf(0x7F, 0x45, 0x4C, 0x46)

private val PE_MAGIC: ByteArray = byteArrayOf(0x4D, 0x5A)

private val MACH_O_MAGICS: List<ByteArray> = listOf(
    byteArrayOf(0xFE.toByte(), 0xED.toByte(), 0xFA.toByte(), 0xCE.toByte()),
    byteArrayOf(0xFE.toByte(), 0xED.toByte(), 0xFA.toByte(), 0xCF.toByte()),
    byteArrayOf(0xCE.toByte(), 0xFA.toByte(), 0xED.toByte(), 0xFE.toByte()),
    byteArrayOf(0xCF.toByte(), 0xFA.toByte(), 0xED.toByte(), 0xFE.toByte())
)

private val UNIVERSAL_MACH_O_MAGIC: ByteArray = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte())

private val ZIP_MAGIC: ByteArray = byteArrayOf(0x50, 0x4B, 0x03, 0x04)

private val NATIVE_EXTENSIONS: Set<String> = setOf("so", "dll", "dylib", "jnilib", "exe", "node")

private const val FAT_ARCHITECTURES_OFFSET: Int = 7

private const val MAX_FAT_ARCHITECTURES: Int = 30

internal object BinaryMagic {
    fun isNative(name: String, bytes: ByteArray): Boolean {
        val extension = name.substringAfterLast('.', "").lowercase()
        if (extension in NATIVE_EXTENSIONS) return true
        if (bytes.startsWith(ELF_MAGIC) || bytes.startsWith(PE_MAGIC)) return !isClass(name)
        if (MACH_O_MAGICS.any { magic -> bytes.startsWith(magic) }) return true
        return isUniversalBinary(name, bytes)
    }

    fun isArchive(bytes: ByteArray): Boolean = bytes.startsWith(ZIP_MAGIC)

    private fun isClass(name: String): Boolean = name.endsWith(".class")

    private fun isUniversalBinary(name: String, bytes: ByteArray): Boolean {
        if (isClass(name) || !bytes.startsWith(UNIVERSAL_MACH_O_MAGIC)) return false

        val architectures = bytes.getOrNull(FAT_ARCHITECTURES_OFFSET)?.toInt() ?: return false
        val isZeroPadded = (4 until FAT_ARCHITECTURES_OFFSET).all { index -> bytes.getOrNull(index) == 0.toByte() }
        return isZeroPadded && architectures in 1..MAX_FAT_ARCHITECTURES
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { index -> this[index] == prefix[index] }
}
