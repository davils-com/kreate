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
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

private const val FIXTURE_PACKAGE = "com/davils/kreate/api/abi/fixtures"
private const val MARKER = "com.davils.kreate.api.abi.fixtures.HiddenFixture"
private const val PLAIN_JAVA_CLASS = "org/objectweb/asm/Opcodes"

private fun AbiClass.memberNames(): List<String> = members.map { it.name }

class AbiExtractorTest : FunSpec({
    val testClassLoader: ClassLoader = AbiExtractorTest::class.java.classLoader

    fun bytecodeOf(resource: String): ByteArray {
        val stream = checkNotNull(testClassLoader.getResourceAsStream(resource)) {
            "Fixture class $resource is not on the test classpath."
        }
        return stream.use { it.readBytes() }
    }

    fun extract(
        vararg simpleNames: String,
        options: AbiFilterOptions = AbiFilterOptions(nonPublicMarkers = setOf(MARKER))
    ): List<AbiClass> {
        val bytecode = simpleNames.map { name -> bytecodeOf("$FIXTURE_PACKAGE/$name.class") }
        return AbiExtractor.extract(bytecode, options)
    }

    context("ABI extraction") {
        test("keeps public and protected members and drops private ones") {
            val fixture = extract("VisibilityFixture").single()

            fixture.memberNames() shouldContain "publicFunction"
            fixture.memberNames() shouldContain "protectedFunction"
            fixture.memberNames() shouldContain "getPublicProperty"
            fixture.memberNames() shouldNotContain "privateFunction"
            fixture.memberNames() shouldNotContain "getPrivateProperty"
        }

        test("drops Kotlin internal members even though the bytecode calls them public") {
            val fixture = extract("VisibilityFixture").single()
            val memberNames = fixture.memberNames()

            memberNames.none { mangledName -> mangledName.startsWith("internalFunction") } shouldBe true
            memberNames.none { mangledName -> mangledName.startsWith("getInternalProperty") } shouldBe true
        }

        test("drops members carrying a marker annotation") {
            val fixture = extract("VisibilityFixture").single()

            fixture.memberNames() shouldNotContain "markedFunction"
        }

        test("keeps a marked member when no marker is configured") {
            val fixture = extract("VisibilityFixture", options = AbiFilterOptions()).single()

            fixture.memberNames() shouldContain "markedFunction"
        }

        test("drops a Kotlin internal class entirely") {
            extract("InternalFixture") shouldBe emptyList()
        }

        test("keeps an internal class published for inline functions") {
            val fixture = extract("PublishedClassFixture").single()

            fixture.memberNames() shouldContain "function"
            fixture.memberNames().none { it.startsWith("internalFunction") } shouldBe true
        }

        test("keeps internal members published for inline functions") {
            val fixture = extract("PublishedMembersFixture").single()

            fixture.memberNames() shouldContain "publishedFunction"
            fixture.memberNames() shouldContain "getPublishedProperty"
            fixture.memberNames() shouldContain "publishedWithDefault"
            fixture.memberNames() shouldContain "publishedWithDefault\$default"
            fixture.members.count { it.name == "<init>" } shouldBe 2
        }

        test("still drops internal members that are not published") {
            val fixture = extract("PublishedMembersFixture").single()

            fixture.memberNames().none { it.startsWith("internalFunction") } shouldBe true
            fixture.memberNames().none { it.endsWith("\$annotations") } shouldBe true
        }

        test("drops a class hidden by a marker annotation") {
            extract("MarkedFixture") shouldBe emptyList()
        }

        test("drops a nested class whose outer class is hidden") {
            val extracted = extract(
                "OuterFixture",
                "OuterFixture\$NestedFixture",
                "OuterFixture\$InternalNestedFixture",
                "OuterFixture\$InternalNestedFixture\$DeeplyNestedFixture"
            ).map { it.internalName }
            val publicByOwnFlagsButUnderHiddenOuter =
                "$FIXTURE_PACKAGE/OuterFixture\$InternalNestedFixture\$DeeplyNestedFixture"

            extracted shouldContain "$FIXTURE_PACKAGE/OuterFixture"
            extracted shouldContain "$FIXTURE_PACKAGE/OuterFixture\$NestedFixture"
            extracted shouldNotContain "$FIXTURE_PACKAGE/OuterFixture\$InternalNestedFixture"
            extracted shouldNotContain publicByOwnFlagsButUnderHiddenOuter
        }

        test("honours ignoredPackages and ignoredClasses") {
            val byPackage = extract(
                "VisibilityFixture",
                options = AbiFilterOptions(
                    ignoredPackages = setOf("com.davils.kreate.api")
                )
            )
            byPackage shouldBe emptyList()

            val byClass = extract(
                "VisibilityFixture",
                options = AbiFilterOptions(
                    ignoredClasses = setOf("com.davils.kreate.api.abi.fixtures.VisibilityFixture")
                )
            )
            byClass shouldBe emptyList()
        }

        test("records supertypes and the interface keyword") {
            val rendered = AbiRenderer.render(extract("InterfaceFixture"))

            rendered shouldContain "public abstract interface $FIXTURE_PACKAGE/InterfaceFixture {"
            rendered shouldContain "public abstract fun abstractFunction ()V"
        }

        test("records an enum with its generated static members") {
            val rendered = AbiRenderer.render(extract("EnumFixture"))
            val privateSyntheticValuesField = "\$VALUES"

            rendered shouldContain "public final class $FIXTURE_PACKAGE/EnumFixture : java/lang/Enum {"
            rendered shouldContain "public static final field FIRST L$FIXTURE_PACKAGE/EnumFixture;"
            rendered shouldContain "public static fun values ()[L$FIXTURE_PACKAGE/EnumFixture;"
            rendered.contains(privateSyntheticValuesField) shouldBe false
        }

        test("reads a class without Kotlin metadata rather than failing") {
            val bytecode = bytecodeOf("$PLAIN_JAVA_CLASS.class")

            val extracted = AbiExtractor.extract(listOf(bytecode), AbiFilterOptions()).single()

            extracted.internalName shouldBe PLAIN_JAVA_CLASS
            extracted.members shouldNotBe emptyList<AbiMember>()
        }
    }
})
