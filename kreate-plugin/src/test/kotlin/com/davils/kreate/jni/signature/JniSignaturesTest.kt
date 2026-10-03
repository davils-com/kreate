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

package com.davils.kreate.jni.signature

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private val ARGUMENT_TYPES: List<Pair<String, String>> = listOf(
    "Z" to "jboolean",
    "B" to "jbyte",
    "C" to "jchar",
    "S" to "jshort",
    "I" to "jint",
    "J" to "jlong",
    "F" to "jfloat",
    "D" to "jdouble",
    "Ljava/lang/String;" to "jstring",
    "Ljava/lang/Class;" to "jclass",
    "Ljava/lang/Throwable;" to "jthrowable",
    "Ljava/util/List;" to "jobject",
    "[Z" to "jbooleanArray",
    "[I" to "jintArray",
    "[Ljava/lang/String;" to "jobjectArray",
    "[[I" to "jobjectArray"
)

private val RETURN_TYPES: List<Pair<String, String>> = listOf(
    "V" to "void",
    "I" to "jint",
    "Ljava/lang/String;" to "jstring",
    "[B" to "jbyteArray"
)

class JniSignaturesTest : FunSpec({
    context("JniSignatures") {
        context("name mangling") {
            test("uses the short form for a uniquely named method") {
                val method = NativeMethod(
                    "com/davils/example/JNI",
                    "hello",
                    "()Ljava/lang/String;",
                    isStatic = false
                )

                JniSignatures.mangledName(method, useLongForm = false) shouldBe
                    "Java_com_davils_example_JNI_hello"
            }

            test("appends the argument signature for overloaded methods") {
                val method = NativeMethod("com/example/Foo", "bar", "(ILjava/lang/String;)V", isStatic = false)

                JniSignatures.mangledName(method, useLongForm = true) shouldBe
                    "Java_com_example_Foo_bar__ILjava_lang_String_2"
            }

            test("escapes underscores so that they cannot collide with package separators") {
                val wouldCollideWithPackageABDotC = NativeMethod("a_b/C", "do_it", "()V", isStatic = false)

                JniSignatures.mangledName(wouldCollideWithPackageABDotC, useLongForm = false) shouldBe
                    "Java_a_1b_C_do_1it"
            }

            test("escapes non-ASCII characters as _0xxxx") {
                val method = NativeMethod("com/example/Foo", "grüßen", "()V", isStatic = false)
                val umlautUAsU00fcAndSharpSAsU00df = "Java_com_example_Foo_gr_000fc_000dfen"

                JniSignatures.mangledName(method, useLongForm = false) shouldBe umlautUAsU00fcAndSharpSAsU00df
            }

            test("escapes array markers in the long form") {
                val method = NativeMethod("com/example/Foo", "bar", "([I)V", isStatic = false)

                JniSignatures.mangledName(method, useLongForm = true) shouldBe "Java_com_example_Foo_bar___3I"
            }
        }

        context("declaration rendering") {
            test("passes a jobject receiver for instance methods") {
                val declarations = JniSignatures.renderDeclarations(
                    listOf(NativeMethod("com/example/Foo", "bar", "()V", isStatic = false))
                )

                declarations.single() shouldContain "(JNIEnv *env, jobject receiver)"
            }

            test("passes a jclass receiver for static methods") {
                val declarations = JniSignatures.renderDeclarations(
                    listOf(NativeMethod("com/example/Foo", "bar", "()V", isStatic = true))
                )

                declarations.single() shouldContain "(JNIEnv *env, jclass receiver)"
            }

            test("switches every overload of a name to the long form") {
                val declarations = JniSignatures.renderDeclarations(
                    listOf(
                        NativeMethod("com/example/Foo", "bar", "(I)V", isStatic = false),
                        NativeMethod("com/example/Foo", "bar", "(J)V", isStatic = false)
                    )
                )

                declarations[0] shouldContain "Java_com_example_Foo_bar__I"
                declarations[1] shouldContain "Java_com_example_Foo_bar__J"
            }

            test("keeps the short form when names do not collide") {
                val declarations = JniSignatures.renderDeclarations(
                    listOf(
                        NativeMethod("com/example/Foo", "bar", "(I)V", isStatic = false),
                        NativeMethod("com/example/Foo", "baz", "(J)V", isStatic = false)
                    )
                )

                declarations[0] shouldContain "JNICALL Java_com_example_Foo_bar\n"
                declarations[1] shouldContain "JNICALL Java_com_example_Foo_baz\n"
            }

            test("numbers the declared arguments after the receiver") {
                val declarations = JniSignatures.renderDeclarations(
                    listOf(NativeMethod("com/example/Foo", "bar", "(IJ)V", isStatic = false))
                )

                declarations.single() shouldContain "(JNIEnv *env, jobject receiver, jint arg1, jlong arg2)"
            }

            test("documents the originating class, method and descriptor") {
                val declarations = JniSignatures.renderDeclarations(
                    listOf(NativeMethod("com/example/Foo", "bar", "()I", isStatic = false))
                )

                declarations.single() shouldContain "Class:     com.example.Foo"
                declarations.single() shouldContain "Signature: ()I"
            }
        }

        context("descriptor to JNI type mapping") {
            withData(
                nameFn = { (descriptor, expected) -> "$descriptor maps to $expected" },
                ARGUMENT_TYPES
            ) { (descriptor, expected) ->
                val declarations = JniSignatures.renderDeclarations(
                    listOf(NativeMethod("com/example/Foo", "bar", "($descriptor)V", isStatic = false))
                )

                declarations.single() shouldContain "$expected arg1"
            }

            withData(
                nameFn = { (descriptor, expected) -> "return $descriptor maps to $expected" },
                RETURN_TYPES
            ) { (descriptor, expected) ->
                val declarations = JniSignatures.renderDeclarations(
                    listOf(NativeMethod("com/example/Foo", "bar", "()$descriptor", isStatic = false))
                )

                declarations.single() shouldContain "JNIEXPORT $expected JNICALL"
            }
        }
    }
})
