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

import com.davils.kreate.archive.ArchiveEntryKind
import com.davils.kreate.archive.TarHeader
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

private const val TAR_BLOCK_BYTES: Int = 512

internal object TrivyArchive {
    fun extract(archive: Path, asset: TrivyAsset, target: Path) {
        val found = Files.newInputStream(archive).use { input -> extractFrom(input, asset, target) }
        if (!found) throw TrivyUnavailableException("The Trivy archive holds no ${asset.binaryName}.")
    }

    private fun extractFrom(input: InputStream, asset: TrivyAsset, target: Path): Boolean {
        if (asset.isZip) return extractFromZip(input, asset.binaryName, target)
        return extractFromTar(GZIPInputStream(input), asset.binaryName, target)
    }

    private fun extractFromZip(input: InputStream, binaryName: String, target: Path): Boolean {
        val zip = ZipInputStream(input)
        var entry = zip.nextEntry
        while (entry != null) {
            if (entry.name == binaryName) {
                Files.newOutputStream(target).use { output -> zip.copyTo(output) }
                return true
            }
            entry = zip.nextEntry
        }
        return false
    }

    private fun extractFromTar(input: InputStream, binaryName: String, target: Path): Boolean {
        var block = input.readNBytes(TAR_BLOCK_BYTES)
        while (block.size == TAR_BLOCK_BYTES && block.any { byte -> byte != 0.toByte() }) {
            val header = TarHeader.parse(block)
            if (header.kind == ArchiveEntryKind.FILE && header.name == binaryName) {
                copyBounded(input, header.size, target)
                return true
            }
            input.skipNBytes(paddedSize(header.size))
            block = input.readNBytes(TAR_BLOCK_BYTES)
        }
        return false
    }

    private fun copyBounded(input: InputStream, size: Long, target: Path) {
        val bounded = BoundedInput(input, size)
        Files.newOutputStream(target).use { output -> bounded.copyTo(output) }
    }

    private fun paddedSize(size: Long): Long {
        val overhang = size % TAR_BLOCK_BYTES
        if (overhang == 0L) return size
        return size + TAR_BLOCK_BYTES - overhang
    }
}
