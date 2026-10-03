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

package com.davils.kreate.rules.kdoc

import dev.detekt.test.utils.compileContentForTest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

class ApiVisibilityTest : FunSpec({

    context("apiVisibility") {
        test("is public for a declaration without modifier") {
            declaration("public class Exchanger", "Exchanger").apiVisibility() shouldBe ApiVisibility.PUBLIC
        }

        test("is hidden for an internal declaration") {
            declaration("internal class Exchanger", "Exchanger").apiVisibility() shouldBe ApiVisibility.HIDDEN
        }

        test("is hidden for a public member of a private class") {
            val source = "private class Exchanger { public fun exchange(): Unit = Unit }"

            declaration(source, "exchange").apiVisibility() shouldBe ApiVisibility.HIDDEN
        }

        test("is protected for a protected member of a public class") {
            val source = "public open class Exchanger { protected fun exchange(): Unit = Unit }"

            declaration(source, "exchange").apiVisibility() shouldBe ApiVisibility.PROTECTED
        }

        test("is hidden for a declaration inside a function body") {
            val source = "public fun run() { class Helper }"

            declaration(source, "Helper").apiVisibility() shouldBe ApiVisibility.HIDDEN
        }
    }
})

private fun declaration(source: String, name: String): KtDeclaration {
    val file = compileContentForTest(source)
    val declarations = file.collectDescendantsOfType<KtNamedDeclaration>()
    return declarations.single { declaration -> declaration.name == name }
}
