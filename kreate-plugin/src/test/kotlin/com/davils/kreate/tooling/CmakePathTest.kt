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

package com.davils.kreate.tooling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

class CmakePathTest : FunSpec({
    context("CMake path conversion") {
        test("converts a Windows JDK path into a form the CMake language can parse") {
            val javaHomeFromTheCiFailure = """C:\hostedtoolcache\windows\Java_Temurin-Hotspot_jdk\17.0.20-8\x64"""

            javaHomeFromTheCiFailure.toCmakePath() shouldBe
                "C:/hostedtoolcache/windows/Java_Temurin-Hotspot_jdk/17.0.20-8/x64"
        }

        test("leaves no backslash for CMake to interpret as an escape") {
            val paths = listOf(
                """D:\a\kreate\kreate\example\build\jni\windows-x64\lib""",
                """C:\Program Files\CMake\bin""",
                """\\server\share\includes"""
            )

            paths.forEach { it.toCmakePath() shouldNotContain "\\" }
        }

        test("is a no-op on paths that already use forward slashes") {
            val unixPath = "/usr/lib/jvm/temurin-17"

            unixPath.toCmakePath() shouldBe unixPath
        }

        test("is idempotent") {
            val path = """C:\Users\build\project"""
            val once = path.toCmakePath()

            once.toCmakePath() shouldBe once
        }

        test("preserves the drive letter and the segment separator semantics") {
            """C:\build""".toCmakePath() shouldBe "C:/build"
        }
    }
})
