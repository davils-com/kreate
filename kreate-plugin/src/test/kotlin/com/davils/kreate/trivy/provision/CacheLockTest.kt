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

import com.davils.kreate.freshDirectory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private const val HOLDERS: Int = 8

private const val HOLD_MILLIS: Long = 20

private const val WAIT_SECONDS: Long = 30

class CacheLockTest : FunSpec({
    val workspace = tempdir()

    fun guardedDirectory(): Path {
        val cache = workspace.freshDirectory("cache")
        return cache.toPath().resolve("Linux-64bit")
    }

    test("lets one holder in at a time, however many threads ask") {
        val directory = guardedDirectory()
        val inside = AtomicInteger()
        val mostInside = AtomicInteger()
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(HOLDERS)

        val holders = List(HOLDERS) {
            pool.submit {
                start.await()
                CacheLock.holding(directory) {
                    mostInside.accumulateAndGet(inside.incrementAndGet(), ::maxOf)
                    Thread.sleep(HOLD_MILLIS)
                    inside.decrementAndGet()
                }
            }
        }
        start.countDown()
        holders.forEach { holder -> holder.get(WAIT_SECONDS, TimeUnit.SECONDS) }
        pool.shutdown()

        mostInside.get() shouldBe 1
    }

    test("keeps its lock file beside the directory it guards") {
        val directory = guardedDirectory()

        CacheLock.holding(directory) { Files.createDirectories(directory) }

        CacheLock.lockFileOf(directory) shouldBe directory.resolveSibling("Linux-64bit.lock")
        Files.list(directory).use { entries -> entries.count() } shouldBe 0
    }

    test("gives up on a holder that does not finish") {
        val directory = guardedDirectory()
        val held = CountDownLatch(1)
        val release = CountDownLatch(1)
        val pool = Executors.newSingleThreadExecutor()
        val holder = pool.submit {
            CacheLock.holding(directory) {
                held.countDown()
                release.await()
            }
        }
        held.await()

        val failure = shouldThrow<TrivyUnavailableException> {
            CacheLock.holding(directory, Duration.ofMillis(HOLD_MILLIS * 10)) {}
        }
        release.countDown()
        holder.get(WAIT_SECONDS, TimeUnit.SECONDS)
        pool.shutdown()

        failure.message.orEmpty() shouldContain "Another build"
    }
})
