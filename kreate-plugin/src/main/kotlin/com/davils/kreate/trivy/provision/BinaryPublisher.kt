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

import com.davils.kreate.digest.Sha256
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

private const val DIGEST_SUFFIX: String = ".sha256"

private fun atomicMove(source: Path, target: Path) {
    Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
}

internal class BinaryPublisher(
    private val warn: (String) -> Unit,
    private val move: (Path, Path) -> Unit = ::atomicMove
) {
    fun publish(binary: Path, target: Path): Path {
        val digest = Sha256.hexOf(binary)
        record(binary, target, digest)
        if (holds(target, digest)) return target

        try {
            move(binary, target)
        } catch (cause: AtomicMoveNotSupportedException) {
            throw TrivyUnavailableException("The Trivy cache does not support an atomic move: ${cause.message}", cause)
        } catch (cause: FileSystemException) {
            return settle(target, digest, cause)
        }
        return target
    }

    private fun record(binary: Path, target: Path, digest: String) {
        val staged = digestFileOf(binary)
        Files.writeString(staged, digest)
        try {
            atomicMove(staged, digestFileOf(target))
        } catch (cause: FileSystemException) {
            throw TrivyUnavailableException("The digest of $target could not be written: ${cause.message}", cause)
        }
    }

    private fun settle(target: Path, digest: String, cause: FileSystemException): Path {
        if (!holds(target, digest)) {
            throw TrivyUnavailableException("$target exists and could not be replaced: ${cause.message}", cause)
        }
        warn("Another build installed the same Trivy at $target at the same time.")
        return target
    }

    private fun holds(target: Path, digest: String): Boolean {
        if (!Files.isRegularFile(target)) return false
        return Sha256.hexOf(target) == digest
    }

    companion object {
        fun digestFileOf(binary: Path): Path = binary.resolveSibling(binary.fileName.toString() + DIGEST_SUFFIX)
    }
}
