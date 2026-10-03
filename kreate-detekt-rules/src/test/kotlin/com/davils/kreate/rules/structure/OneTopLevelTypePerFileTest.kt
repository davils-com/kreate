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

package com.davils.kreate.rules.structure

import dev.detekt.api.Config
import dev.detekt.test.lint
import dev.detekt.test.utils.compileContentForTest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize

class OneTopLevelTypePerFileTest : FunSpec({

    context("reports") {
        test("a second top-level type") {
            val file = compileContentForTest(
                """
                    public class Exchanger

                    public class Key
                """.trimIndent(),
                "Exchanger.kt"
            )

            val findings = OneTopLevelTypePerFile(Config.empty).lint(file)

            findings shouldHaveSize 1
        }

        test("a private helper type next to the primary one") {
            val file = compileContentForTest(
                """
                    public class Exchanger

                    private data class Handshake(val id: String)
                """.trimIndent(),
                "Exchanger.kt"
            )

            val findings = OneTopLevelTypePerFile(Config.empty).lint(file)

            findings shouldHaveSize 1
        }

        test("a type alias next to a class") {
            val file = compileContentForTest(
                """
                    public class Exchanger

                    public typealias Exchangers = List<Exchanger>
                """.trimIndent(),
                "Exchanger.kt"
            )

            val findings = OneTopLevelTypePerFile(Config.empty).lint(file)

            findings shouldHaveSize 1
        }

        test("a file not named after its type") {
            val file = compileContentForTest(
                """
                    public class Exchanger
                """.trimIndent(),
                "Utils.kt"
            )

            val findings = OneTopLevelTypePerFile(Config.empty).lint(file)

            findings shouldHaveSize 1
        }

        test("a function file whose name is not PascalCase") {
            val file = compileContentForTest(
                """
                    public fun exchange(): Unit = Unit
                """.trimIndent(),
                "exchange.kt"
            )

            val findings = OneTopLevelTypePerFile(Config.empty).lint(file)

            findings shouldHaveSize 1
        }

        test("a platform file not named after its type") {
            val file = compileContentForTest(
                """
                    public class Foo
                """.trimIndent(),
                "Bar.jvm.kt"
            )

            val findings = OneTopLevelTypePerFile(Config.empty).lint(file)

            findings shouldHaveSize 1
        }

        test("a platform function file whose name is not PascalCase") {
            val file = compileContentForTest(
                """
                    public actual fun platformName(): String = "android"
                """.trimIndent(),
                "platform.android.kt"
            )

            val findings = OneTopLevelTypePerFile(Config.empty).lint(file)

            findings shouldHaveSize 1
        }
    }

    context("leaves alone") {
        test("an actual type in a JVM platform file") {
            val file = compileContentForTest(
                """
                    public actual class Foo
                """.trimIndent(),
                "Foo.jvm.kt"
            )

            val findings = OneTopLevelTypePerFile(Config.empty).lint(file)

            findings shouldHaveSize 0
        }

        test("an actual type in a native platform file") {
            val file = compileContentForTest(
                """
                    public actual class Foo
                """.trimIndent(),
                "Foo.native.kt"
            )

            val findings = OneTopLevelTypePerFile(Config.empty).lint(file)

            findings shouldHaveSize 0
        }

        test("a platform function file named after its role") {
            val file = compileContentForTest(
                """
                    public actual fun platformName(): String = "android"

                    public actual fun platformVersion(): Int = 34
                """.trimIndent(),
                "Platform.android.kt"
            )

            val findings = OneTopLevelTypePerFile(Config.empty).lint(file)

            findings shouldHaveSize 0
        }

        test("one type in a file named after it") {
            val file = compileContentForTest(
                """
                    public sealed interface Exchanger {
                        public data object Idle : Exchanger
                        public data class Busy(val peer: String) : Exchanger
                    }
                """.trimIndent(),
                "Exchanger.kt"
            )

            val findings = OneTopLevelTypePerFile(Config.empty).lint(file)

            findings shouldHaveSize 0
        }

        test("functions and constants next to the type") {
            val file = compileContentForTest(
                """
                    public const val DEFAULT_PEER: String = "local"

                    public enum class Exchanger { DIRECT, RELAYED }

                    public fun Exchanger.isDirect(): Boolean = this == Exchanger.DIRECT
                """.trimIndent(),
                "Exchanger.kt"
            )

            val findings = OneTopLevelTypePerFile(Config.empty).lint(file)

            findings shouldHaveSize 0
        }

        test("a function file named after its role") {
            val file = compileContentForTest(
                """
                    public fun exchange(): Unit = Unit
                """.trimIndent(),
                "KeyExchange.kt"
            )

            val findings = OneTopLevelTypePerFile(Config.empty).lint(file)

            findings shouldHaveSize 0
        }

        test("a build script") {
            val file = compileContentForTest(
                """
                    class Helper

                    class Other
                """.trimIndent(),
                "build.gradle.kts"
            )

            val findings = OneTopLevelTypePerFile(Config.empty).lint(file)

            findings shouldHaveSize 0
        }
    }
})
