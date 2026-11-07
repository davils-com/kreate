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
package com.davils.kreate.inspection.scan

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class StringEntropyTest : FunSpec({
    context("a string") {
        test("is suspicious when it looks like a key") {
            StringEntropy.isSuspicious("xK9mQ2vL7pR4tZ8wB3nJ6yH1cF5dG0sA") shouldBe true
        }

        mapOf(
            "too short" to "aB3dE5",
            "a sentence" to "a perfectly ordinary sentence with 12 words",
            "a method descriptor" to "(Ljava/lang/String;)Lcom/example/deep/package/Value3;",
            "a regular expression" to "([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)",
            "a format string" to "Value(idType=%s,includeAs=%s,propertyName=%s)",
            "an identifier" to "copyToRecursivelyImplementationFunctionName",
            "an alphabet" to "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        ).forEach { (kind, text) ->
            test("is not suspicious when it is $kind") {
                StringEntropy.isSuspicious(text) shouldBe false
            }
        }
    }
})
