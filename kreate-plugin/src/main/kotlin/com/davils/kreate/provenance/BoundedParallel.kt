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
package com.davils.kreate.provenance

import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future

internal fun <T, R> boundedMap(items: List<T>, threads: Int, transform: (T) -> R): List<R> {
    if (items.isEmpty()) return emptyList()
    val pool = Executors.newFixedThreadPool(threads.coerceIn(1, items.size))
    try {
        val futures = items.map { item -> pool.submit(Callable { transform(item) }) }
        return futures.map { future -> resultOf(future) }
    } finally {
        pool.shutdownNow()
    }
}

private fun <R> resultOf(future: Future<R>): R = try {
    future.get()
} catch (failure: ExecutionException) {
    throw failure.cause ?: failure
}
