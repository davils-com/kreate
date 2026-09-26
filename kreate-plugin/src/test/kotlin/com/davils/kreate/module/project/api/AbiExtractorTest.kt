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

package com.davils.kreate.module.project.api

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

private const val FIXTURE_PACKAGE = "com/davils/kreate/module/project/api/fixtures"
private const val MARKER = "com.davils.kreate.module.project.api.fixtures.HiddenFixture"

@DisplayName("ABI extraction")
class AbiExtractorTest {

    private fun extract(
        vararg simpleNames: String,
        options: AbiFilterOptions = AbiFilterOptions(nonPublicMarkers = setOf(MARKER))
    ): List<AbiClass> {
        val bytecode = simpleNames.map { name ->
            val resource = "$FIXTURE_PACKAGE/$name.class"
            checkNotNull(javaClass.classLoader.getResourceAsStream(resource)) {
                "Fixture class $resource is not on the test classpath."
            }.use { it.readBytes() }
        }
        return AbiExtractor.extract(bytecode, options)
    }

    private fun AbiClass.memberNames(): List<String> = members.map { it.name }

    @Test
    @DisplayName("keeps public and protected members and drops private ones")
    fun keepsExposedMembers() {
        val fixture = extract("VisibilityFixture").single()

        fixture.memberNames() shouldContain "publicFunction"
        fixture.memberNames() shouldContain "protectedFunction"
        fixture.memberNames() shouldContain "getPublicProperty"
        fixture.memberNames() shouldNotContain "privateFunction"
        fixture.memberNames() shouldNotContain "getPrivateProperty"
    }

    @Test
    @DisplayName("drops Kotlin internal members even though the bytecode calls them public")
    fun dropsInternalMembers() {
        val fixture = extract("VisibilityFixture").single()

        // The mangled name is what would otherwise leak: `internalFunction$kreate_plugin`.
        fixture.memberNames().none { it.startsWith("internalFunction") } shouldBe true
        fixture.memberNames().none { it.startsWith("getInternalProperty") } shouldBe true
    }

    @Test
    @DisplayName("drops members carrying a marker annotation")
    fun dropsMarkedMembers() {
        val fixture = extract("VisibilityFixture").single()

        fixture.memberNames() shouldNotContain "markedFunction"
    }

    @Test
    @DisplayName("keeps a marked member when no marker is configured")
    fun keepsMarkedWhenUnconfigured() {
        val fixture = extract("VisibilityFixture", options = AbiFilterOptions()).single()

        fixture.memberNames() shouldContain "markedFunction"
    }

    @Test
    @DisplayName("drops a Kotlin internal class entirely")
    fun dropsInternalClass() {
        extract("InternalFixture") shouldBe emptyList()
    }

    @Test
    @DisplayName("keeps an internal class published for inline functions")
    fun keepsPublishedClass() {
        val fixture = extract("PublishedClassFixture").single()

        fixture.memberNames() shouldContain "function"
        fixture.memberNames().none { it.startsWith("internalFunction") } shouldBe true
    }

    @Test
    @DisplayName("keeps internal members published for inline functions")
    fun keepsPublishedMembers() {
        val fixture = extract("PublishedMembersFixture").single()

        fixture.memberNames() shouldContain "publishedFunction"
        fixture.memberNames() shouldContain "getPublishedProperty"
        fixture.memberNames() shouldContain "publishedWithDefault"
        fixture.memberNames() shouldContain "publishedWithDefault\$default"
        fixture.members.count { it.name == "<init>" } shouldBe 2
    }

    @Test
    @DisplayName("still drops internal members that are not published")
    fun dropsUnpublishedMembers() {
        val fixture = extract("PublishedMembersFixture").single()

        fixture.memberNames().none { it.startsWith("internalFunction") } shouldBe true
        fixture.memberNames().none { it.endsWith("\$annotations") } shouldBe true
    }

    @Test
    @DisplayName("drops a class hidden by a marker annotation")
    fun dropsMarkedClass() {
        extract("MarkedFixture") shouldBe emptyList()
    }

    @Test
    @DisplayName("drops a nested class whose outer class is hidden")
    fun dropsNestedClassOfHiddenOuter() {
        val extracted = extract(
            "OuterFixture",
            "OuterFixture\$NestedFixture",
            "OuterFixture\$InternalNestedFixture",
            "OuterFixture\$InternalNestedFixture\$DeeplyNestedFixture"
        ).map { it.internalName }

        extracted shouldContain "$FIXTURE_PACKAGE/OuterFixture"
        extracted shouldContain "$FIXTURE_PACKAGE/OuterFixture\$NestedFixture"
        extracted shouldNotContain "$FIXTURE_PACKAGE/OuterFixture\$InternalNestedFixture"
        // Its own flags say public; only the hidden outer class rules it out.
        extracted shouldNotContain
            "$FIXTURE_PACKAGE/OuterFixture\$InternalNestedFixture\$DeeplyNestedFixture"
    }

    @Test
    @DisplayName("honours ignoredPackages and ignoredClasses")
    fun honoursIgnoreLists() {
        val byPackage = extract(
            "VisibilityFixture",
            options = AbiFilterOptions(
                ignoredPackages = setOf("com.davils.kreate.module.project.api")
            )
        )
        byPackage shouldBe emptyList()

        val byClass = extract(
            "VisibilityFixture",
            options = AbiFilterOptions(
                ignoredClasses = setOf("com.davils.kreate.module.project.api.fixtures.VisibilityFixture")
            )
        )
        byClass shouldBe emptyList()
    }

    @Test
    @DisplayName("records supertypes and the interface keyword")
    fun recordsSupertypes() {
        val rendered = AbiRenderer.render(extract("InterfaceFixture"))

        rendered shouldContain "public abstract interface $FIXTURE_PACKAGE/InterfaceFixture {"
        rendered shouldContain "public abstract fun abstractFunction ()V"
    }

    @Test
    @DisplayName("records an enum with its generated static members")
    fun recordsEnum() {
        val rendered = AbiRenderer.render(extract("EnumFixture"))

        rendered shouldContain "public final class $FIXTURE_PACKAGE/EnumFixture : java/lang/Enum {"
        rendered shouldContain "public static final field FIRST L$FIXTURE_PACKAGE/EnumFixture;"
        rendered shouldContain "public static fun values ()[L$FIXTURE_PACKAGE/EnumFixture;"
        // `$VALUES` is private synthetic and must never reach the dump.
        rendered.contains("\$VALUES") shouldBe false
    }

    @Test
    @DisplayName("reads a class without Kotlin metadata rather than failing")
    fun readsPlainJavaClass() {
        val bytecode = checkNotNull(
            javaClass.classLoader.getResourceAsStream("org/objectweb/asm/Opcodes.class")
        ).use { it.readBytes() }

        val extracted = AbiExtractor.extract(listOf(bytecode), AbiFilterOptions()).single()

        extracted.internalName shouldBe "org/objectweb/asm/Opcodes"
        extracted.members shouldNotBe emptyList<AbiMember>()
    }
}
