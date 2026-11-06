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

package com.davils.kreate.net

import java.time.Duration
import kotlin.random.Random

private const val MAX_BACKOFF_EXPONENT: Int = 32

private val RETRYABLE_STATUS_CODES: Set<Int> = setOf(408, 425, 429, 500, 502, 503, 504)

internal class RetryPolicy(
    val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val baseDelay: Duration = DEFAULT_BASE_DELAY,
    private val maxDelay: Duration = DEFAULT_MAX_DELAY,
    private val random: Random = Random.Default
) {
    fun isRetryableStatus(statusCode: Int): Boolean = statusCode in RETRYABLE_STATUS_CODES

    fun delayFor(attempt: Int, retryAfter: Duration?): Duration {
        if (retryAfter != null) return minOf(retryAfter, maxDelay)

        val exponent = (attempt - 1).coerceIn(0, MAX_BACKOFF_EXPONENT)
        val backOff = baseDelay.multipliedBy(1L shl exponent)
        val cappedMillis = minOf(backOff, maxDelay).toMillis()
        val halfMillis = cappedMillis / 2
        val jitteredMillis = random.nextLong(halfMillis, cappedMillis + 1)
        return Duration.ofMillis(jitteredMillis)
    }

    companion object {
        const val DEFAULT_MAX_ATTEMPTS: Int = 3

        val DEFAULT_BASE_DELAY: Duration = Duration.ofSeconds(1)

        val DEFAULT_MAX_DELAY: Duration = Duration.ofSeconds(30)
    }
}
