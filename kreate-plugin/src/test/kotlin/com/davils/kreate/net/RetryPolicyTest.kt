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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import java.time.Duration
import kotlin.random.Random

class RetryPolicyTest : FunSpec({
    val policy = RetryPolicy(baseDelay = Duration.ofSeconds(1), random = Random(42))

    test("defaults to the Inspectra semantics") {
        val defaults = RetryPolicy()

        defaults.maxAttempts shouldBe 3
        RetryPolicy.DEFAULT_BASE_DELAY shouldBe Duration.ofSeconds(1)
        RetryPolicy.DEFAULT_MAX_DELAY shouldBe Duration.ofSeconds(30)
    }

    test("retries the transient status codes only") {
        val statuses = (100..599).filter(policy::isRetryableStatus)

        statuses shouldBe listOf(408, 425, 429, 500, 502, 503, 504)
    }

    test("keeps the jitter between half and the full back-off") {
        repeat(200) {
            policy.delayFor(1, retryAfter = null).toMillis() shouldBeInRange 500L..1000L
            policy.delayFor(2, retryAfter = null).toMillis() shouldBeInRange 1000L..2000L
            policy.delayFor(3, retryAfter = null).toMillis() shouldBeInRange 2000L..4000L
        }
    }

    test("reaches both ends of the jitter range") {
        val shortPolicy = RetryPolicy(baseDelay = Duration.ofMillis(4))

        val delays = List(2_000) { shortPolicy.delayFor(1, null).toMillis() }

        delays.toSet() shouldBe setOf(2L, 3L, 4L)
    }

    test("caps the back-off at thirty seconds") {
        repeat(50) {
            policy.delayFor(10, retryAfter = null).toMillis() shouldBeInRange 15_000L..30_000L
        }
    }

    test("does not overflow for absurd attempt numbers") {
        policy.delayFor(Int.MAX_VALUE, retryAfter = null).toMillis() shouldBeInRange 15_000L..30_000L
    }

    test("treats attempts below one like the first") {
        policy.delayFor(0, retryAfter = null).toMillis() shouldBeInRange 500L..1000L
    }

    test("lets Retry-After win") {
        policy.delayFor(1, Duration.ofSeconds(7)) shouldBe Duration.ofSeconds(7)
        policy.delayFor(3, Duration.ZERO) shouldBe Duration.ZERO
    }

    test("caps Retry-After at thirty seconds") {
        policy.delayFor(1, Duration.ofSeconds(3_600)) shouldBe Duration.ofSeconds(30)
    }

    test("has no delay for a zero base") {
        RetryPolicy(baseDelay = Duration.ZERO).delayFor(1, null) shouldBe Duration.ZERO
    }
})
