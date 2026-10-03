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

package com.davils.kreate.jni.task

import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest

internal const val DIGEST_MANIFEST_FILE_NAME: String = "digests.properties"

private const val DIGEST_ALGORITHM: String = "SHA-256"
private const val READ_BUFFER: Int = 64 * 1024
private const val HEXADECIMAL: String = "0123456789abcdef"
private const val NIBBLE: Int = 4
private const val LOW_NIBBLE: Int = 0xF
private const val BYTE_MASK: Int = 0xFF

@CacheableTask
internal abstract class GenerateDigestManifest : KreateTask(
    "Writes the SHA-256 of each packaged native library.",
    KreateTaskGroup.JNI
) {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val libraryDirectory: DirectoryProperty

    @get:Input
    public abstract val platformId: Property<String>

    @get:OutputFile
    public abstract val manifest: RegularFileProperty

    @TaskAction
    public fun execute() {
        val directoryEntries = libraryDirectory.get().asFile.listFiles()
        val libraries = directoryEntries.orEmpty().filter { file -> file.isFile }
        val target = manifest.get().asFile
        target.parentFile.mkdirs()

        target.writeText(renderManifest(libraries))
    }

    private fun renderManifest(libraries: List<File>): String = buildString {
        appendLine("# Written by Kreate. Do not edit - it is rewritten on every build.")
        appendLine("# Each value is the SHA-256 of the native library published for that platform.")
        appendLine("#")
        appendLine("# This is the digest to pin in a consumer's declaration. Read out of the same")
        appendLine("# artifact as the binary it describes it proves nothing: whoever replaced one")
        appendLine("# replaced the other.")
        for (library in libraries.sortedBy { file -> file.name }) {
            appendLine("${platformId.get()}=${digestOf(library)}")
        }
    }

    private fun digestOf(file: File): String {
        val digest = MessageDigest.getInstance(DIGEST_ALGORITHM)
        file.inputStream().use { source ->
            val buffer = ByteArray(READ_BUFFER)
            var read = source.read(buffer)
            while (read >= 0) {
                digest.update(buffer, 0, read)
                read = source.read(buffer)
            }
        }

        return toHexadecimal(digest.digest())
    }

    private fun toHexadecimal(digest: ByteArray): String {
        val rendered = StringBuilder(digest.size * 2)
        for (byte in digest) {
            val value = byte.toInt() and BYTE_MASK
            rendered.append(HEXADECIMAL[value ushr NIBBLE])
            rendered.append(HEXADECIMAL[value and LOW_NIBBLE])
        }

        return rendered.toString()
    }
}
