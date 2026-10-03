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

package com.davils.kreate.jni

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class JniTaskNamesTest : FunSpec({
    context("JniTaskNames") {
        test("the native JAR task name follows the kreate scheme") {
            JniTaskNames.nativeJar("linux-x64") shouldBe "kreateJniNativeJarLinuxX64"
        }

        test("the native JAR task name of a known platform keeps the architecture spelling") {
            JniTaskNames.nativeJar("linux-x86_64") shouldBe "kreateJniNativeJarLinuxX86_64"
        }
    }
})
