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

package com.davils.kreate.module.project.api.fixtures

/**
 * Marks a declaration as hidden from the extracted binary interface.
 */
@Retention(AnnotationRetention.BINARY)
annotation class HiddenFixture

/**
 * A class whose members cover every visibility the extractor has to distinguish.
 */
open class VisibilityFixture {
    val publicProperty: Int = 1

    internal val internalProperty: Int = 2

    private val privateProperty: Int = 3

    fun publicFunction() = privateProperty

    internal fun internalFunction() = Unit

    private fun privateFunction() = Unit

    protected fun protectedFunction() = Unit

    @HiddenFixture
    fun markedFunction() = Unit
}

/**
 * A class that is not visible outside its module and must never be dumped.
 */
internal class InternalFixture {
    fun function() = Unit
}

/**
 * A class with a nested type, used to check that the nesting chain is honoured.
 */
class OuterFixture {
    class NestedFixture

    internal class InternalNestedFixture {
        class DeeplyNestedFixture
    }
}

/**
 * A class hidden by a marker annotation, along with everything nested inside it.
 */
@HiddenFixture
class MarkedFixture {
    class NestedInMarkedFixture
}

/**
 * An interface, to cover the `interface` keyword and abstract members.
 */
interface InterfaceFixture {
    fun abstractFunction()
}

/**
 * An enum, to cover the synthesised static members and the `java/lang/Enum` supertype.
 */
enum class EnumFixture {
    FIRST,
    SECOND
}

/**
 * A class that is `internal` in Kotlin but published for inline functions, so it belongs in the
 * binary interface.
 */
@PublishedApi
internal class PublishedClassFixture {
    fun function() = Unit

    internal fun internalFunction() = Unit
}

/**
 * A class whose `internal` members are published for inline functions, next to one that is not.
 */
class PublishedMembersFixture @PublishedApi internal constructor(value: String) {
    @PublishedApi
    internal val publishedProperty: Int = value.length

    constructor() : this("")

    @PublishedApi
    internal fun publishedFunction() = Unit

    @PublishedApi
    internal fun publishedWithDefault(value: Int = 1) = value

    internal fun internalFunction() = Unit

    inline fun inlined(offset: () -> Int): Int = offset() + publishedProperty + publishedWithDefault()
}
