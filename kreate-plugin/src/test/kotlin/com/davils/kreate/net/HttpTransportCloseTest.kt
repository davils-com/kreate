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
import io.kotest.matchers.shouldBe

class HttpTransportCloseTest : FunSpec({
    context("closing a transport") {
        test("reaches the transport behind the redirects") {
            val inner = ClosableTransport()

            RedirectingTransport(inner).close()

            inner.isClosed shouldBe true
        }

        test("reaches the transport behind the retries") {
            val inner = ClosableTransport()

            RetryingTransport(inner, RetryPolicy(), RecordingSleeper()).close()

            inner.isClosed shouldBe true
        }

        test("is harmless for a transport that holds nothing") {
            val transport = HttpTransport { _ -> error("Not called by this test.") }

            transport.close()
        }
    }
})
