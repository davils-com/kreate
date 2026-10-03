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

import com.davils.kreate.api.metadata.KotlinDeclarationFilter
import com.davils.kreate.api.metadata.KotlinMetadataValues
import com.davils.kreate.api.metadata.PublishedApiDeclarations
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

internal object AbiExtractor {
    private const val ASM_API = Opcodes.ASM9
    private const val PARSING_OPTIONS =
        ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
    private const val KOTLIN_METADATA_DESCRIPTOR = "Lkotlin/Metadata;"
    private const val PUBLISHED_API_DESCRIPTOR = "Lkotlin/PublishedApi;"
    private const val PROPERTY_ANNOTATIONS_SUFFIX = "\$annotations"
    private const val SYNTHETIC_ACCESSOR_PREFIX = "access\$"
    private const val CONSTRUCTOR_NAME = "<init>"
    private const val DUMMY_CONSTRUCTOR_DESCRIPTOR = "(Lkotlin/jvm/internal/DefaultConstructorMarker;)V"
    private const val NESTED_CLASS_SEPARATOR = '$'
    private const val INTERNAL_NAME_SEPARATOR = '/'
    private const val PACKAGE_SEPARATOR = '.'

    private const val CLASS_KIND = 1
    private const val KIND_KEY = "k"
    private const val EXTRA_INT_KEY = "xi"
    private const val EXTRA_STRING_KEY = "xs"
    private const val PACKAGE_NAME_KEY = "pn"
    private const val METADATA_VERSION_KEY = "mv"
    private const val DATA1_KEY = "d1"
    private const val DATA2_KEY = "d2"

    fun extract(classFiles: Iterable<ByteArray>, options: AbiFilterOptions): List<AbiClass> {
        val extracted = classFiles.mapNotNull { bytecode -> extractClass(bytecode, options) }
        val visibleNames = extracted.mapTo(mutableSetOf()) { abiClass -> abiClass.internalName }
        return extracted.filter { abiClass -> hasVisibleOuterClasses(abiClass.internalName, visibleNames) }
    }

    private fun extractClass(bytecode: ByteArray, options: AbiFilterOptions): AbiClass? {
        val visitor = AbiClassVisitor(options)
        val reader = ClassReader(bytecode)
        reader.accept(visitor, PARSING_OPTIONS)
        return visitor.result
    }

    private fun hasVisibleOuterClasses(internalName: String, visibleNames: Set<String>): Boolean {
        var outer = outerClassOf(internalName)
        while (outer.isNotEmpty()) {
            if (outer !in visibleNames) return false
            outer = outerClassOf(outer)
        }
        return true
    }

    private fun outerClassOf(internalName: String): String =
        internalName.substringBeforeLast(NESTED_CLASS_SEPARATOR, missingDelimiterValue = "")

    private fun isExposed(access: Int): Boolean =
        access and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED) != 0

    private fun isSynthetic(access: Int): Boolean = access and Opcodes.ACC_SYNTHETIC != 0

    private fun isCompilerPlumbing(name: String, descriptor: String, access: Int): Boolean {
        if (!isSynthetic(access)) return false

        val isSyntheticAccessor = name.startsWith(SYNTHETIC_ACCESSOR_PREFIX)
        val isPropertyAnnotationsHolder = name.endsWith(PROPERTY_ANNOTATIONS_SUFFIX)
        val isDummyConstructor = name == CONSTRUCTOR_NAME && descriptor == DUMMY_CONSTRUCTOR_DESCRIPTOR
        return isSyntheticAccessor || isPropertyAnnotationsHolder || isDummyConstructor
    }

    private fun isPropertyAnnotationHolder(name: String, access: Int): Boolean =
        isSynthetic(access) && name.endsWith(PROPERTY_ANNOTATIONS_SUFFIX)

    private class AbiClassVisitor(
        private val options: AbiFilterOptions
    ) : ClassVisitor(ASM_API) {
        private var access = 0
        private var internalName = ""
        private var superName: String? = null
        private var interfaces: List<String> = emptyList()
        private var isLocalOrAnonymous = false
        private var isMarkedNonPublic = false
        private var isPublishedApi = false
        private var metadata: KotlinMetadataValues? = null
        private val members = mutableListOf<PendingMember>()

        private val publishedApiMethods = mutableSetOf<String>()

        var result: AbiClass? = null
            private set

        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
        ) {
            this.access = access
            this.internalName = name
            this.superName = superName
            this.interfaces = interfaces?.toList().orEmpty()
        }

        override fun visitOuterClass(owner: String, name: String?, descriptor: String?) {
            isLocalOrAnonymous = true
        }

        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
            if (descriptor in options.markerDescriptors) isMarkedNonPublic = true
            if (descriptor == PUBLISHED_API_DESCRIPTOR) isPublishedApi = true
            if (descriptor != KOTLIN_METADATA_DESCRIPTOR) return null

            return KotlinMetadataVisitor { values -> metadata = values }
        }

        override fun visitField(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            value: Any?
        ): FieldVisitor? {
            if (!isExposed(access)) return null

            val pending = PendingMember(AbiMemberKind.FIELD, access, name, descriptor)
            members += pending
            return MarkerFieldVisitor(pending, options)
        }

        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor? {
            val key = KotlinDeclarationFilter.memberKey(name, descriptor)
            if (isPropertyAnnotationHolder(name, access)) {
                return PublishedApiMethodVisitor { publishedApiMethods += key }
            }
            if (!isExposed(access) || isCompilerPlumbing(name, descriptor, access)) return null

            val pending = PendingMember(AbiMemberKind.METHOD, access, name, descriptor)
            members += pending
            return MarkerMethodVisitor(pending, options) { publishedApiMethods += key }
        }

        override fun visitEnd() {
            val published = PublishedApiDeclarations(isPublishedApi, publishedApiMethods)
            val filter = KotlinDeclarationFilter.from(metadata, published)
            if (isExcluded(filter)) return

            val visibleMembers = visibleMembers(filter)
            val isEmptyFileFacade = filter.isFileFacade && visibleMembers.isEmpty()
            if (isEmptyFileFacade) return

            result = AbiClass(
                access = access,
                internalName = internalName,
                superName = superName,
                interfaces = interfaces,
                members = visibleMembers
            )
        }

        private fun visibleMembers(filter: KotlinDeclarationFilter): List<AbiMember> {
            val unmarked = members.filterNot { member -> member.isMarkedNonPublic }
            val public = unmarked.filterNot { member -> filter.isNonPublicMember(member.name, member.descriptor) }
            return public.map { member -> AbiMember(member.kind, member.access, member.name, member.descriptor) }
        }

        private fun isExcluded(filter: KotlinDeclarationFilter): Boolean =
            isHiddenByDeclaration(filter) || isIgnoredByConfiguration()

        private fun isHiddenByDeclaration(filter: KotlinDeclarationFilter): Boolean =
            !isExposed(access) ||
                isLocalOrAnonymous ||
                isMarkedNonPublic ||
                isSynthetic(access) ||
                filter.isNonPublicClass

        private fun isIgnoredByConfiguration(): Boolean {
            val className = internalName.replace(INTERNAL_NAME_SEPARATOR, PACKAGE_SEPARATOR)
            return className in options.ignoredClasses || isInIgnoredPackage(className)
        }

        private fun isInIgnoredPackage(className: String): Boolean {
            val packageName = className.substringBeforeLast(PACKAGE_SEPARATOR, missingDelimiterValue = "")
            return options.ignoredPackages.any { ignored ->
                packageName == ignored || packageName.startsWith("$ignored$PACKAGE_SEPARATOR")
            }
        }
    }

    private class PendingMember(
        val kind: AbiMemberKind,
        val access: Int,
        val name: String,
        val descriptor: String
    ) {
        var isMarkedNonPublic: Boolean = false
    }

    private class MarkerFieldVisitor(
        private val member: PendingMember,
        private val options: AbiFilterOptions
    ) : FieldVisitor(ASM_API) {
        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
            if (descriptor in options.markerDescriptors) member.isMarkedNonPublic = true
            return null
        }
    }

    private class MarkerMethodVisitor(
        private val member: PendingMember,
        private val options: AbiFilterOptions,
        private val onPublishedApi: () -> Unit
    ) : MethodVisitor(ASM_API) {
        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
            if (descriptor in options.markerDescriptors) member.isMarkedNonPublic = true
            if (descriptor == PUBLISHED_API_DESCRIPTOR) onPublishedApi()
            return null
        }
    }

    private class PublishedApiMethodVisitor(
        private val onPublishedApi: () -> Unit
    ) : MethodVisitor(ASM_API) {
        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
            if (descriptor == PUBLISHED_API_DESCRIPTOR) onPublishedApi()
            return null
        }
    }

    private class KotlinMetadataVisitor(
        private val onComplete: (KotlinMetadataValues) -> Unit
    ) : AnnotationVisitor(ASM_API) {
        private var kind = CLASS_KIND
        private val metadataVersion = mutableListOf<Int>()
        private val data1 = mutableListOf<String>()
        private val data2 = mutableListOf<String>()
        private var extraString = ""
        private var packageName = ""
        private var extraInt = 0

        override fun visit(name: String?, value: Any?) {
            when (name) {
                KIND_KEY -> kind = value as? Int ?: kind
                EXTRA_INT_KEY -> extraInt = value as? Int ?: extraInt
                EXTRA_STRING_KEY -> extraString = value as? String ?: extraString
                PACKAGE_NAME_KEY -> packageName = value as? String ?: packageName
                METADATA_VERSION_KEY -> appendPrimitiveArrayVersion(value)
            }
        }

        override fun visitArray(name: String?): AnnotationVisitor {
            when (name) {
                METADATA_VERSION_KEY -> return ArrayCollector { element -> metadataVersion += element as Int }
                DATA1_KEY -> return ArrayCollector { element -> data1 += element as String }
                DATA2_KEY -> return ArrayCollector { element -> data2 += element as String }
            }
            return ArrayCollector { }
        }

        override fun visitEnd() {
            onComplete(
                KotlinMetadataValues(
                    kind = kind,
                    metadataVersion = metadataVersion.toIntArray(),
                    data1 = data1.toTypedArray(),
                    data2 = data2.toTypedArray(),
                    extraString = extraString,
                    packageName = packageName,
                    extraInt = extraInt
                )
            )
        }

        private fun appendPrimitiveArrayVersion(value: Any?) {
            val version = value as? IntArray ?: return
            metadataVersion += version.toList()
        }

        private class ArrayCollector(private val onValue: (Any) -> Unit) : AnnotationVisitor(ASM_API) {
            override fun visit(name: String?, value: Any?) {
                value?.let(onValue)
            }
        }
    }
}
