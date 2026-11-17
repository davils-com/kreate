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

import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration

private const val LOCK_SUFFIX: String = ".lock"

private val POLL_INTERVAL: Duration = Duration.ofMillis(100)

private val DEFAULT_PATIENCE: Duration = Duration.ofMinutes(15)

internal object CacheLock {
    fun <T> holding(directory: Path, patience: Duration = DEFAULT_PATIENCE, action: () -> T): T {
        val lockFile = lockFileOf(directory)
        Files.createDirectories(lockFile.parent)

        return FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
            acquire(channel, directory, patience).use { action() }
        }
    }

    fun lockFileOf(directory: Path): Path = directory.resolveSibling(directory.fileName.toString() + LOCK_SUFFIX)

    private fun acquire(channel: FileChannel, directory: Path, patience: Duration): FileLock {
        val deadline = System.nanoTime() + patience.toNanos()
        var lock = tryAcquire(channel)
        while (lock == null) {
            val overdue = System.nanoTime() - deadline
            if (overdue > 0) {
                throw TrivyUnavailableException(
                    "Another build has been installing Trivy into $directory for longer than $patience."
                )
            }
            Thread.sleep(POLL_INTERVAL)
            lock = tryAcquire(channel)
        }
        return lock
    }

    private fun tryAcquire(channel: FileChannel): FileLock? =
        try {
            channel.tryLock()
        } catch (_: OverlappingFileLockException) {
            null
        }
}
