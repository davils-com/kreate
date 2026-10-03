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

package com.davils.kreate.api.abi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class JvmDescriptorsTest : FunSpec({
    context("JVM descriptors") {
        test("splits primitives, objects and arrays") {
            JvmDescriptors.parameterTypes("(ILjava/lang/String;[[BLjava/io/File;)V") shouldBe
                listOf("I", "Ljava/lang/String;", "[[B", "Ljava/io/File;")
        }

        test("returns an empty list for a descriptor without parameters") {
            JvmDescriptors.parameterTypes("()V") shouldBe emptyList()
        }

        test("returns an empty list rather than throwing on a malformed descriptor") {
            JvmDescriptors.parameterTypes("Ljava/lang/String;") shouldBe emptyList()
            JvmDescriptors.parameterTypes("(Ljava/lang/String)V") shouldBe emptyList()
            JvmDescriptors.parameterTypes("([)V") shouldBe emptyList()
        }

        test("extracts the return type") {
            JvmDescriptors.returnType("(I)Ljava/lang/String;") shouldBe "Ljava/lang/String;"
            JvmDescriptors.returnType("()V") shouldBe "V"
            JvmDescriptors.returnType("nonsense") shouldBe ""
        }
    }
})
