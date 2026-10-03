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

package com.davils.kreate.trivy.lockfile

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private val LOCK_FILE: List<String> = listOf(
    "# This is a Gradle generated file for dependency locking.",
    "com.example:shipped:1.0.0=compileClasspath,runtimeClasspath",
    "com.example:runtime-only:1.0.0=runtimeClasspath,unitTestRuntimeClasspath",
    "io.kotest:kotest-runner-junit5:6.2.4=unitTestCompileClasspath,unitTestRuntimeClasspath",
    "net.java.dev.jna:jna:5.9.0=unitTestRuntimeClasspath",
    "empty=annotationProcessor,unitTestAnnotationProcessor",
    ""
)

class LockFileFilterTest : FunSpec({
    context("retainConfigurations") {
        test("keeps the dependencies of the given configurations and the comments") {
            val retained = retainConfigurations(LOCK_FILE, setOf("compileClasspath", "runtimeClasspath"))

            retained shouldContainExactly listOf(
                "# This is a Gradle generated file for dependency locking.",
                "com.example:shipped:1.0.0=compileClasspath,runtimeClasspath",
                "com.example:runtime-only:1.0.0=runtimeClasspath,unitTestRuntimeClasspath"
            )
        }

        test("drops a dependency that only a test suite resolves") {
            val retained = retainConfigurations(LOCK_FILE, setOf("runtimeClasspath"))

            val hasTestOnlyDependency = retained.any { line -> line.startsWith("net.java.dev.jna") }
            hasTestOnlyDependency shouldBe false
        }

        test("keeps only the comments when no configuration matches") {
            val retained = retainConfigurations(LOCK_FILE, setOf("jvmRuntimeClasspath"))

            retained shouldContainExactly listOf("# This is a Gradle generated file for dependency locking.")
        }

        test("keeps a test dependency when its configuration is named explicitly") {
            val retained = retainConfigurations(LOCK_FILE, setOf("unitTestRuntimeClasspath"))

            retained shouldContainExactly listOf(
                "# This is a Gradle generated file for dependency locking.",
                "com.example:runtime-only:1.0.0=runtimeClasspath,unitTestRuntimeClasspath",
                "io.kotest:kotest-runner-junit5:6.2.4=unitTestCompileClasspath,unitTestRuntimeClasspath",
                "net.java.dev.jna:jna:5.9.0=unitTestRuntimeClasspath"
            )
        }
    }
})
