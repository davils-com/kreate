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

package com.davils.kreate.api.metadata

import com.davils.kreate.api.abi.JvmDescriptors
import kotlin.metadata.KmClass
import kotlin.metadata.KmDeclarationContainer
import kotlin.metadata.KmPackage
import kotlin.metadata.KmProperty
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

internal class KotlinDeclarationFilter private constructor(
    val isNonPublicClass: Boolean,
    val isFileFacade: Boolean,
    private val nonPublicMembers: Set<String>
) {
    fun isNonPublicMember(name: String, descriptor: String): Boolean =
        memberKey(name, descriptor) in nonPublicMembers || isNonPublicDefaultBridge(name, descriptor)

    private fun isNonPublicDefaultBridge(name: String, descriptor: String): Boolean {
        if (!name.endsWith(DEFAULT_BRIDGE_SUFFIX)) return false

        val parameters = JvmDescriptors.parameterTypes(descriptor)
        val hasBridgeShape = parameters.size >= DEFAULT_BRIDGE_EXTRA_PARAMETERS &&
            parameters.getOrNull(parameters.size - DEFAULT_BRIDGE_EXTRA_PARAMETERS) == INT_DESCRIPTOR &&
            parameters.lastOrNull() == OBJECT_DESCRIPTOR

        val bridged = name.removeSuffix(DEFAULT_BRIDGE_SUFFIX)
        val returnType = JvmDescriptors.returnType(descriptor)
        val declared = parameters.dropLast(DEFAULT_BRIDGE_EXTRA_PARAMETERS)

        val bridgesNonPublic = receiverVariants(declared).any { parameterList ->
            memberKey(bridged, "(${parameterList.joinToString("")})$returnType") in nonPublicMembers
        }

        return hasBridgeShape && bridgesNonPublic
    }

    private fun receiverVariants(declared: List<String>): List<List<String>> {
        if (declared.isEmpty()) return listOf(declared)
        return listOf(declared, declared.drop(1))
    }

    internal companion object {
        private const val DEFAULT_BRIDGE_SUFFIX = "\$default"
        private const val OBJECT_DESCRIPTOR = "Ljava/lang/Object;"
        private const val INT_DESCRIPTOR = "I"
        private const val DEFAULT_BRIDGE_EXTRA_PARAMETERS = 2

        val PERMISSIVE: KotlinDeclarationFilter =
            KotlinDeclarationFilter(isNonPublicClass = false, isFileFacade = false, nonPublicMembers = emptySet())

        private val NON_PUBLIC_VISIBILITIES = setOf(
            Visibility.INTERNAL,
            Visibility.PRIVATE,
            Visibility.PRIVATE_TO_THIS,
            Visibility.LOCAL
        )

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
            val parsed = readLenientlyOrNull(metadata) ?: return PERMISSIVE

            return when (parsed) {
                is KotlinClassMetadata.Class -> fromClass(parsed.kmClass, published)
                is KotlinClassMetadata.FileFacade -> fromPackage(parsed.kmPackage, published)
                is KotlinClassMetadata.MultiFileClassPart -> fromPackage(parsed.kmPackage, published)
                is KotlinClassMetadata.MultiFileClassFacade -> emptyFileFacade()
                is KotlinClassMetadata.SyntheticClass -> PERMISSIVE
                is KotlinClassMetadata.Unknown -> PERMISSIVE
            }
        }

        fun memberKey(name: String, descriptor: String): String = "$name $descriptor"

        private fun readLenientlyOrNull(metadata: Metadata): KotlinClassMetadata? =
            try {
                KotlinClassMetadata.readLenient(metadata)
            } catch (ignored: IllegalArgumentException) {
                null
            }

        private fun emptyFileFacade(): KotlinDeclarationFilter = KotlinDeclarationFilter(
            isNonPublicClass = false,
            isFileFacade = true,
            nonPublicMembers = emptySet()
        )

        private fun fromClass(kmClass: KmClass, published: PublishedApiDeclarations): KotlinDeclarationFilter {
            val nonPublicConstructors = kmClass.constructors.filter { it.visibility in NON_PUBLIC_VISIBILITIES }
            val constructorSignatures = nonPublicConstructors.mapNotNull { it.signature }
            val members = collectNonPublicMembers(kmClass, published) +
                unpublishedKeys(constructorSignatures, published)

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
            val nonPublicFunctions = container.functions.filter { it.visibility in NON_PUBLIC_VISIBILITIES }
            val functionSignatures = nonPublicFunctions.mapNotNull { it.signature }

            val nonPublicProperties = container.properties.filter { it.visibility in NON_PUBLIC_VISIBILITIES }
            val unpublishedProperties = nonPublicProperties.filterNot { property ->
                property.syntheticMethodForAnnotations?.asKey() in published.methods
            }

            return unpublishedKeys(functionSignatures, published) +
                unpublishedProperties.flatMap { property -> propertyKeys(property) }
        }

        private fun unpublishedKeys(
            signatures: List<JvmMethodSignature>,
            published: PublishedApiDeclarations
        ): Set<String> {
            val keys = signatures.map { signature -> signature.asKey() }
            return keys.filterNotTo(mutableSetOf()) { key -> key in published.methods }
        }

        private fun propertyKeys(property: KmProperty): List<String> {
            val accessorKeys = listOfNotNull(property.getterSignature, property.setterSignature).map { it.asKey() }
            val fieldKey = property.fieldSignature?.asKey()
            return accessorKeys + listOfNotNull(fieldKey)
        }

        private fun JvmMethodSignature.asKey(): String = memberKey(name, descriptor)

        private fun JvmFieldSignature.asKey(): String = memberKey(name, descriptor)
    }
}
