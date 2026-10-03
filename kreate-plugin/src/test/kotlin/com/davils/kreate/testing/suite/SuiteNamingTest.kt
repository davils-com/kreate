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

package com.davils.kreate.testing.suite

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SuiteNamingTest : FunSpec({

    context("lower camel case names") {

        test("the first part is lowercased and every later part capitalised") {
            lowerCamelCaseName("Jvm", "unit", "test") shouldBe "jvmUnitTest"
        }

        test("missing and empty parts are skipped") {
            lowerCamelCaseName(null, "", "integration", "test") shouldBe "integrationTest"
        }

        test("no parts give an empty name") {
            lowerCamelCaseName() shouldBe ""
        }
    }

    context("suite names") {

        test("a suite's shared multiplatform source set is prefixed with common") {
            sharedSourceSetName("unitTest") shouldBe "commonUnitTest"
        }

        test("a target's suite task carries the target classifier") {
            targetTestTaskName("jvm", "unitTest") shouldBe "jvmUnitTest"
        }

        test("a target without a classifier keeps the suite's own name") {
            targetTestTaskName(null, "integrationTest") shouldBe "integrationTest"
        }

        test("configuration names follow the source set name") {
            implementationName("unitTest") shouldBe "unitTestImplementation"
            compileOnlyName("unitTest") shouldBe "unitTestCompileOnly"
            runtimeOnlyName("unitTest") shouldBe "unitTestRuntimeOnly"
        }
    }
})
