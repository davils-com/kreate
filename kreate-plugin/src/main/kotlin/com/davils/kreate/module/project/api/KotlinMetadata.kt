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

import kotlin.metadata.KmClass
import kotlin.metadata.KmDeclarationContainer
import kotlin.metadata.KmPackage
import kotlin.metadata.Visibility
import kotlin.metadata.jvm.JvmFieldSignature
import kotlin.metadata.jvm.JvmMethodSignature
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.fieldSignature
import kotlin.metadata.jvm.getterSignature
import kotlin.metadata.jvm.setterSignature
import kotlin.metadata.jvm.signature
import kotlin.metadata.jvm.syntheticMethodForAnnotations
import kotlin.metadata.visibility

/**
 * The raw contents of a `kotlin.Metadata` annotation, as collected from a class file.
 *
 * @since 2.1.0
 */
internal data class KotlinMetadataValues(
    /**
     * The `k` field: the kind of class the metadata describes.
     * @since 2.1.0
     */
    val kind: Int,
    /**
     * The `mv` field: the metadata version the class was compiled with.
     * @since 2.1.0
     */
    val metadataVersion: IntArray,
    /**
     * The `d1` field: the encoded protobuf payload.
     * @since 2.1.0
     */
    val data1: Array<String>,
    /**
     * The `d2` field: the string table referenced by the payload.
     * @since 2.1.0
     */
    val data2: Array<String>,
    /**
     * The `xs` field: an extra string whose meaning depends on [kind].
     * @since 2.1.0
     */
    val extraString: String,
    /**
     * The `pn` field: the package name, when it differs from the JVM package.
     * @since 2.1.0
     */
    val packageName: String,
    /**
     * The `xi` field: extra flags.
     * @since 2.1.0
     */
    val extraInt: Int
) {

    /**
     * Compares two instances by value, including the array fields.
     *
     * @param other The instance to compare against.
     * @return `true` when every field is equal.
     * @since 2.1.0
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KotlinMetadataValues) return false

        return kind == other.kind &&
            metadataVersion.contentEquals(other.metadataVersion) &&
            data1.contentEquals(other.data1) &&
            data2.contentEquals(other.data2) &&
            extraString == other.extraString &&
            packageName == other.packageName &&
            extraInt == other.extraInt
    }

    /**
     * Computes a hash consistent with [equals].
     *
     * @return The hash code.
     * @since 2.1.0
     */
    override fun hashCode(): Int {
        var result = kind
        result = 31 * result + metadataVersion.contentHashCode()
        result = 31 * result + data1.contentHashCode()
        result = 31 * result + data2.contentHashCode()
        result = 31 * result + extraString.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + extraInt
        return result
    }
}

/**
 * The declarations of a class that carry `@PublishedApi`.
 *
 * `@PublishedApi internal` is part of the binary interface although Kotlin callers cannot name
 * it: a public inline function calls it from the consumer's own bytecode, so renaming or
 * removing it breaks every consumer compiled against the old version. The annotation has binary
 * retention and is read from the class file rather than from the Kotlin metadata, which does not
 * record it.
 *
 * @since 3.5.0
 */
internal data class PublishedApiDeclarations(
    /**
     * Whether the class itself carries `@PublishedApi`.
     * @since 3.5.0
     */
    val isPublishedClass: Boolean = false,
    /**
     * The `name descriptor` keys of the methods carrying `@PublishedApi`. For a property this is
     * the synthetic method Kotlin puts the property's annotations on.
     * @since 3.5.0
     */
    val methods: Set<String> = emptySet()
)

/**
 * Decides which declarations of a class are hidden from Kotlin callers even though the
 * bytecode marks them `public`.
 *
 * This is the whole reason ABI validation cannot work from access flags alone. A Kotlin
 * `internal` function compiles to a `public` method with a mangled name, and an
 * `internal` class to a `public` class. Dumping those would record declarations no
 * consumer can call, and every rename inside a module would then read as a breaking API
 * change. A declaration marked `@PublishedApi` is the exception: it is `internal` in Kotlin but
 * called from inline functions compiled into the consumer, so it stays in the dump, as it does
 * in the dump of the Kotlin `binary-compatibility-validator` plugin.
 *
 * @since 2.1.0
 */
internal class KotlinDeclarationFilter private constructor(
    /**
     * Whether the class itself is `internal` or `private` in Kotlin.
     * @since 2.1.0
     */
    val isNonPublicClass: Boolean,
    /**
     * Whether the class is a synthesised holder for top level declarations rather than a
     * type a consumer can name.
     *
     * Such a holder only belongs in a dump for the sake of the declarations it carries, so
     * one left with no declarations is dropped entirely — otherwise every file whose top
     * level functions are all `internal` would contribute an empty entry.
     *
     * @since 2.1.0
     */
    val isFileFacade: Boolean,
    /**
     * The JVM signatures, rendered as `name descriptor`, of members that are not public
     * in Kotlin.
     * @since 2.1.0
     */
    private val nonPublicMembers: Set<String>
) {

    /**
     * Reports whether a member must be excluded from the dump.
     *
     * @param name The JVM member name.
     * @param descriptor The JVM member descriptor.
     * @return `true` when the member is not public in Kotlin.
     * @since 2.1.0
     */
    fun isNonPublicMember(name: String, descriptor: String): Boolean =
        memberKey(name, descriptor) in nonPublicMembers || isNonPublicDefaultBridge(name, descriptor)

    /**
     * Reports whether a member is the default argument bridge of a non-public function.
     *
     * Kotlin names the bridge after the function it serves and appends an `int` mask and an
     * `Object` marker to its parameters, but records no metadata of its own. Without this
     * check the bridge of an `internal` function outlives the function in the dump, and
     * every rename inside the module then reads as a change to the public interface.
     *
     * @param name The JVM member name.
     * @param descriptor The JVM member descriptor.
     * @return `true` when the member bridges a function that is not public in Kotlin.
     * @since 2.1.0
     */
    private fun isNonPublicDefaultBridge(name: String, descriptor: String): Boolean {
        if (!name.endsWith(DEFAULT_BRIDGE_SUFFIX)) return false

        val parameters = JvmDescriptors.parameterTypes(descriptor)
        val hasBridgeShape = parameters.size >= DEFAULT_BRIDGE_EXTRA_PARAMETERS &&
            parameters.getOrNull(parameters.size - 2) == "I" &&
            parameters.lastOrNull() == OBJECT_DESCRIPTOR

        val bridged = name.removeSuffix(DEFAULT_BRIDGE_SUFFIX)
        val returnType = JvmDescriptors.returnType(descriptor)
        val declared = parameters.dropLast(DEFAULT_BRIDGE_EXTRA_PARAMETERS)

        // A bridge for an instance function takes the receiver as its first parameter; one
        // for a top level or static function does not. Both shapes are checked because the
        // descriptor alone does not say which it is.
        val candidates = buildList {
            add(declared)
            if (declared.isNotEmpty()) add(declared.drop(1))
        }

        val bridgesNonPublic = candidates.any { parameterList ->
            memberKey(bridged, "(${parameterList.joinToString("")})$returnType") in nonPublicMembers
        }

        return hasBridgeShape && bridgesNonPublic
    }

    internal companion object {
        private const val DEFAULT_BRIDGE_SUFFIX = "\$default"
        private const val OBJECT_DESCRIPTOR = "Ljava/lang/Object;"
        private const val DEFAULT_BRIDGE_EXTRA_PARAMETERS = 2

        /**
         * A filter that hides nothing, used for classes without Kotlin metadata and for
         * metadata this Kotlin version cannot read.
         *
         * @since 2.1.0
         */
        val PERMISSIVE: KotlinDeclarationFilter =
            KotlinDeclarationFilter(isNonPublicClass = false, isFileFacade = false, nonPublicMembers = emptySet())

        private val NON_PUBLIC_VISIBILITIES = setOf(
            Visibility.INTERNAL,
            Visibility.PRIVATE,
            Visibility.PRIVATE_TO_THIS,
            Visibility.LOCAL
        )

        /**
         * Builds a filter from the metadata collected off a class file.
         *
         * Metadata written by a newer Kotlin version than the one Kreate was built
         * against is read leniently, and anything still unreadable degrades to
         * [PERMISSIVE]. Failing the build there would make Kreate the reason a consumer
         * cannot upgrade their compiler; an over-inclusive dump is the milder outcome,
         * and it is visible in the diff.
         *
         * @param values The collected annotation values, or `null` when the class carries
         *   no `kotlin.Metadata`.
         * @param published The declarations of the class that carry `@PublishedApi`.
         * @return A filter for that class.
         * @since 2.1.0
         */
        fun from(
            values: KotlinMetadataValues?,
            published: PublishedApiDeclarations = PublishedApiDeclarations()
        ): KotlinDeclarationFilter {
            if (values == null) return PERMISSIVE

            val metadata = Metadata(
                kind = values.kind,
                metadataVersion = values.metadataVersion,
                data1 = values.data1,
                data2 = values.data2,
                extraString = values.extraString,
                packageName = values.packageName,
                extraInt = values.extraInt
            )

            return runCatching {
                when (val parsed = KotlinClassMetadata.readLenient(metadata)) {
                    is KotlinClassMetadata.Class -> fromClass(parsed.kmClass, published)
                    is KotlinClassMetadata.FileFacade -> fromPackage(parsed.kmPackage, published)
                    is KotlinClassMetadata.MultiFileClassPart -> fromPackage(parsed.kmPackage, published)
                    is KotlinClassMetadata.MultiFileClassFacade -> emptyFileFacade()
                    else -> PERMISSIVE
                }
            }.getOrDefault(PERMISSIVE)
        }

        private fun emptyFileFacade(): KotlinDeclarationFilter = KotlinDeclarationFilter(
            isNonPublicClass = false,
            isFileFacade = true,
            nonPublicMembers = emptySet()
        )

        private fun fromClass(kmClass: KmClass, published: PublishedApiDeclarations): KotlinDeclarationFilter {
            val members = collectNonPublicMembers(kmClass, published).toMutableSet()

            kmClass.constructors
                .filter { it.visibility in NON_PUBLIC_VISIBILITIES }
                .mapNotNull { it.signature }
                .filterNot { it.asKey() in published.methods }
                .forEach { members += it.asKey() }

            val isNonPublicInKotlin = kmClass.visibility in NON_PUBLIC_VISIBILITIES
            return KotlinDeclarationFilter(
                isNonPublicClass = isNonPublicInKotlin && !published.isPublishedClass,
                isFileFacade = false,
                nonPublicMembers = members
            )
        }

        private fun fromPackage(kmPackage: KmPackage, published: PublishedApiDeclarations): KotlinDeclarationFilter =
            KotlinDeclarationFilter(
                isNonPublicClass = false,
                isFileFacade = true,
                nonPublicMembers = collectNonPublicMembers(kmPackage, published)
            )

        private fun collectNonPublicMembers(
            container: KmDeclarationContainer,
            published: PublishedApiDeclarations
        ): Set<String> {
            val members = mutableSetOf<String>()

            container.functions
                .filter { it.visibility in NON_PUBLIC_VISIBILITIES }
                .mapNotNull { it.signature }
                .filterNot { it.asKey() in published.methods }
                .forEach { members += it.asKey() }

            container.properties
                .filter { it.visibility in NON_PUBLIC_VISIBILITIES }
                .filterNot { it.syntheticMethodForAnnotations?.asKey() in published.methods }
                .forEach { property ->
                    listOfNotNull(
                        property.getterSignature,
                        property.setterSignature
                    ).forEach { members += it.asKey() }

                    property.fieldSignature?.let { members += it.asKey() }
                }

            return members
        }

        /**
         * Renders a JVM signature the way [nonPublicMembers] keys it.
         *
         * @return The `name descriptor` key.
         * @since 2.1.0
         */
        private fun JvmMethodSignature.asKey(): String = memberKey(name, descriptor)

        /**
         * Renders a JVM signature the way [nonPublicMembers] keys it.
         *
         * @return The `name descriptor` key.
         * @since 2.1.0
         */
        private fun JvmFieldSignature.asKey(): String = memberKey(name, descriptor)

        /**
         * Builds the key under which a member is looked up.
         *
         * @param name The JVM member name.
         * @param descriptor The JVM member descriptor.
         * @return The `name descriptor` key.
         * @since 2.1.0
         */
        fun memberKey(name: String, descriptor: String): String = "$name $descriptor"
    }
}
