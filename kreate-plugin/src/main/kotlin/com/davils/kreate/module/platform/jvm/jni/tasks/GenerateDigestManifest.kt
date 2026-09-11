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

package com.davils.kreate.module.platform.jvm.jni.tasks

import com.davils.kreate.jobs.Task
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

private const val DIGEST_ALGORITHM: String = "SHA-256"
private const val READ_BUFFER: Int = 64 * 1024
private const val HEXADECIMAL: String = "0123456789abcdef"
private const val NIBBLE: Int = 4
private const val LOW_NIBBLE: Int = 0xF
private const val BYTE_MASK: Int = 0xFF

/**
 * Writes the SHA-256 of every native library this build produced, as a `.properties` document.
 *
 * ```properties
 * # Written by Kreate. Each value is the SHA-256 of the library published for that platform.
 * linux-x86_64=9f2c4d1e8a7b6c5d4e3f2a1b0c9d8e7f6a5b4c3d2e1f0a9b8c7d6e5f4a3b2c1d
 * ```
 *
 * ### What it is for, and what it is not
 *
 * It is **the value a consumer pins**. `com.davils:sira-native` takes a digest per platform, either
 * in the declaration or from a manifest the operator controls, and refuses a binary that does not
 * match. Without this task somebody has to hash the artifact by hand on every release, which is the
 * kind of step that gets skipped once and then never reinstated.
 *
 * It is **not a check that runs itself**, and a loader deliberately does not read it out of the JAR
 * it is checking. A manifest that travels in the same artifact as the binary is written by whoever
 * wrote the binary: it catches a truncated download and proves nothing at all about tampering. The
 * digest has to reach the consumer by a path the publisher does not control - a declaration in their
 * source, or a manifest on a mount they own - before it means anything.
 *
 * @since 3.0.0
 */
@CacheableTask
public abstract class GenerateDigestManifest : Task(
    "Writes the SHA-256 of each packaged native library.",
    "kreate jni"
) {
    /**
     * The directory the native build wrote its libraries to.
     * @since 3.0.0
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val libraryDirectory: DirectoryProperty

    /**
     * The `<os>-<arch>` identifier the libraries in [libraryDirectory] were built for.
     * @since 3.0.0
     */
    @get:Input
    public abstract val platformId: Property<String>

    /**
     * The manifest to write.
     * @since 3.0.0
     */
    @get:OutputFile
    public abstract val manifest: RegularFileProperty

    /**
     * Hashes every library and writes the manifest.
     *
     * @return Unit
     * @since 3.0.0
     */
    @TaskAction
    public fun execute() {
        val libraries = libraryDirectory.get().asFile.listFiles().orEmpty().filter { file -> file.isFile }
        val target = manifest.get().asFile
        target.parentFile.mkdirs()

        target.writeText(render(libraries))
    }

    /**
     * Renders the manifest body.
     *
     * One entry per platform rather than per file: a platform ships one shared library, and a key
     * that carried the file name too would not be the key a loader looks a platform up by.
     *
     * @param libraries The files to hash.
     * @return The manifest contents.
     * @since 3.0.0
     */
    private fun render(libraries: List<File>): String = buildString {
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

    /**
     * The SHA-256 of one file, read as a stream.
     *
     * @param file The file to hash.
     * @return The digest, as 64 lowercase hexadecimal characters.
     * @since 3.0.0
     */
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

        return render(digest.digest())
    }

    /**
     * Renders raw digest bytes as lowercase hexadecimal.
     *
     * @param digest The digest bytes.
     * @return The rendered digest.
     * @since 3.0.0
     */
    private fun render(digest: ByteArray): String {
        val rendered = StringBuilder(digest.size * 2)
        for (byte in digest) {
            val value = byte.toInt() and BYTE_MASK
            rendered.append(HEXADECIMAL[value ushr NIBBLE])
            rendered.append(HEXADECIMAL[value and LOW_NIBBLE])
        }

        return rendered.toString()
    }

    /**
     * Companion object for [GenerateDigestManifest].
     *
     * @since 3.0.0
     */
    public companion object {
        /**
         * The file name the manifest is published under, inside the resource directory.
         * @since 3.0.0
         */
        public const val MANIFEST_FILE_NAME: String = "digests.properties"
    }
}
