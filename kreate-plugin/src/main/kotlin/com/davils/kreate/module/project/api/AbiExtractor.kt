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

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * The options that decide what counts as part of the binary interface.
 *
 * @since 2.1.0
 */
internal data class AbiFilterOptions(
    /**
     * Fully qualified names of annotations that hide the declaration they are applied to.
     * @since 2.1.0
     */
    val nonPublicMarkers: Set<String> = emptySet(),
    /**
     * Package names whose classes are excluded, including their subpackages.
     * @since 2.1.0
     */
    val ignoredPackages: Set<String> = emptySet(),
    /**
     * Fully qualified names of classes to exclude.
     * @since 2.1.0
     */
    val ignoredClasses: Set<String> = emptySet()
) {
    /**
     * The marker annotations as JVM descriptors, which is the form ASM reports.
     * @since 2.1.0
     */
    val markerDescriptors: Set<String> =
        nonPublicMarkers.mapTo(mutableSetOf()) { "L${it.replace('.', '/')};" }
}

/**
 * Extracts the binary interface of a set of compiled classes.
 *
 * The traversal mirrors [com.davils.kreate.module.platform.jvm.jni.tasks.GenerateJniHeaders],
 * which already reads bytecode with ASM: the class body is skipped, because nothing about
 * a method's implementation is part of its binary interface.
 *
 * @since 2.1.0
 */
internal object AbiExtractor {
    private const val ASM_API = Opcodes.ASM9
    private const val PARSING_OPTIONS =
        ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
    private const val KOTLIN_METADATA_DESCRIPTOR = "Lkotlin/Metadata;"
    private const val PUBLISHED_API_DESCRIPTOR = "Lkotlin/PublishedApi;"
    private const val PROPERTY_ANNOTATIONS_SUFFIX = "\$annotations"
    private const val DUMMY_CONSTRUCTOR_DESCRIPTOR = "(Lkotlin/jvm/internal/DefaultConstructorMarker;)V"

    /**
     * Extracts the binary interface from a collection of class files.
     *
     * @param classFiles The raw contents of the class files to read, in any order.
     * @param options The filter options.
     * @return The classes that form the binary interface, unsorted.
     * @since 2.1.0
     */
    fun extract(classFiles: Iterable<ByteArray>, options: AbiFilterOptions): List<AbiClass> {
        val extracted = classFiles.mapNotNull { bytecode -> extractClass(bytecode, options) }

        // A class nested inside a hidden one is unreachable no matter what its own flags
        // say, so it can only be dropped once every class has been seen.
        val visibleNames = extracted.mapTo(mutableSetOf()) { it.internalName }
        return extracted.filter { abiClass -> hasVisibleOuterChain(abiClass.internalName, visibleNames) }
    }

    /**
     * Extracts the binary interface of one class file.
     *
     * @param bytecode The raw class file contents.
     * @param options The filter options.
     * @return The class, or `null` when it is not part of the binary interface.
     * @since 2.1.0
     */
    private fun extractClass(bytecode: ByteArray, options: AbiFilterOptions): AbiClass? {
        val visitor = AbiClassVisitor(options)
        ClassReader(bytecode).accept(visitor, PARSING_OPTIONS)
        return visitor.result
    }

    /**
     * Reports whether every enclosing class of a nested class is itself visible.
     *
     * @param internalName The internal name of the class to check.
     * @param visibleNames The internal names of all classes that passed the filter.
     * @return `true` when the class is top level or all of its outer classes are visible.
     * @since 2.1.0
     */
    private fun hasVisibleOuterChain(internalName: String, visibleNames: Set<String>): Boolean {
        var outer = internalName.substringBeforeLast('$', missingDelimiterValue = "")
        while (outer.isNotEmpty()) {
            if (outer !in visibleNames) return false
            outer = outer.substringBeforeLast('$', missingDelimiterValue = "")
        }
        return true
    }

    /**
     * Reports whether the access flags describe a declaration visible outside its module.
     *
     * Synthetic members are deliberately kept: the bridge method behind an overridden
     * generic function and the extra constructor behind default arguments are both
     * synthetic, both callable, and both part of what a consumer links against.
     *
     * @param access The ASM access flags.
     * @return `true` when the declaration is public or protected.
     * @since 2.1.0
     */
    private fun isExposed(access: Int): Boolean =
        access and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED) != 0

    /**
     * Reports whether a method is compiler plumbing rather than part of the interface.
     *
     * All three shapes are public in the bytecode and none of them is callable from
     * source: the `access$` accessors a lambda uses to reach a private declaration, the
     * `$annotations` holders that carry property annotations, and the marker-only
     * constructor Kotlin emits so that a class with a non-public constructor still has one
     * the runtime can see.
     *
     * @param name The JVM method name.
     * @param descriptor The JVM method descriptor.
     * @param access The ASM access flags.
     * @return `true` when the method is generated plumbing.
     * @since 2.1.0
     */
    private fun isCompilerPlumbing(name: String, descriptor: String, access: Int): Boolean {
        if (access and Opcodes.ACC_SYNTHETIC == 0) return false

        val isDummyConstructor = name == "<init>" && descriptor == DUMMY_CONSTRUCTOR_DESCRIPTOR
        return name.startsWith("access$") || name.endsWith(PROPERTY_ANNOTATIONS_SUFFIX) || isDummyConstructor
    }

    /**
     * Reports whether a method is the synthetic holder Kotlin puts a property's annotations on.
     *
     * The holder is never dumped, but it is the only place a property's `@PublishedApi` can be
     * read from, so it is visited for that one annotation.
     *
     * @param name The JVM method name.
     * @param access The ASM access flags.
     * @return `true` when the method holds the annotations of a property.
     * @since 3.5.0
     */
    private fun isPropertyAnnotationHolder(name: String, access: Int): Boolean =
        access and Opcodes.ACC_SYNTHETIC != 0 && name.endsWith(PROPERTY_ANNOTATIONS_SUFFIX)

    /**
     * Collects the binary interface of a single class while ASM walks it.
     *
     * Members are buffered rather than emitted directly, because the decisions that
     * exclude them — a marker annotation on the member, the Kotlin visibility recorded in
     * the class metadata — are only known once the whole class has been visited.
     *
     * @since 2.1.0
     */
    private class AbiClassVisitor(
        private val options: AbiFilterOptions
    ) : ClassVisitor(ASM_API) {
        private var access = 0
        private var internalName = ""
        private var superName: String? = null
        private var interfaces: List<String> = emptyList()
        private var isEnclosedInMethod = false
        private var isMarkedNonPublic = false
        private var isPublishedApi = false
        private var metadata: KotlinMetadataValues? = null
        private val members = mutableListOf<PendingMember>()

        /**
         * The `name descriptor` keys of the methods carrying `@PublishedApi`, property
         * annotation holders included.
         */
        private val publishedApiMethods = mutableSetOf<String>()

        /**
         * The extracted class, or `null` when it is not part of the binary interface.
         * Only meaningful once ASM has finished visiting.
         *
         * @since 2.1.0
         */
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
            // Set only for local and anonymous classes, which no consumer can name.
            isEnclosedInMethod = true
        }

        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
            if (descriptor in options.markerDescriptors) isMarkedNonPublic = true
            if (descriptor == PUBLISHED_API_DESCRIPTOR) isPublishedApi = true
            if (descriptor != KOTLIN_METADATA_DESCRIPTOR) return null

            return KotlinMetadataVisitor { metadata = it }
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

            val visibleMembers = members
                .filterNot { it.isMarkedNonPublic }
                .filterNot { filter.isNonPublicMember(it.name, it.descriptor) }
                .map { AbiMember(it.kind, it.access, it.name, it.descriptor) }

            // A file facade exists only to hold top level declarations. Once none of them
            // are visible there is no declaration left for a consumer to reach, and an
            // empty entry in the dump would only record which file the source lives in.
            if (!filter.isFileFacade || visibleMembers.isNotEmpty()) {
                result = AbiClass(
                    access = access,
                    internalName = internalName,
                    superName = superName,
                    interfaces = interfaces,
                    members = visibleMembers
                )
            }
        }

        private fun isExcluded(filter: KotlinDeclarationFilter): Boolean =
            isHiddenByDeclaration(filter) || isIgnoredByConfiguration()

        private fun isHiddenByDeclaration(filter: KotlinDeclarationFilter): Boolean = when {
            !isExposed(access) -> true
            isEnclosedInMethod -> true
            isMarkedNonPublic -> true
            access and Opcodes.ACC_SYNTHETIC != 0 -> true
            else -> filter.isNonPublicClass
        }

        private fun isIgnoredByConfiguration(): Boolean {
            val className = internalName.replace('/', '.')
            return className in options.ignoredClasses || isInIgnoredPackage(className)
        }

        private fun isInIgnoredPackage(className: String): Boolean {
            val packageName = className.substringBeforeLast('.', missingDelimiterValue = "")
            return options.ignoredPackages.any { ignored ->
                packageName == ignored || packageName.startsWith("$ignored.")
            }
        }
    }

    /**
     * A member seen by the class visitor, before the exclusion rules have been applied.
     *
     * @since 2.1.0
     */
    private class PendingMember(
        val kind: AbiMemberKind,
        val access: Int,
        val name: String,
        val descriptor: String
    ) {
        /**
         * Whether one of the configured marker annotations is applied to this member.
         * @since 2.1.0
         */
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

    /**
     * Reads the fields of a `kotlin.Metadata` annotation into [KotlinMetadataValues].
     *
     * @since 2.1.0
     */
    private class KotlinMetadataVisitor(
        private val onComplete: (KotlinMetadataValues) -> Unit
    ) : AnnotationVisitor(ASM_API) {
        private var kind = 1
        private val metadataVersion = mutableListOf<Int>()
        private val data1 = mutableListOf<String>()
        private val data2 = mutableListOf<String>()
        private var extraString = ""
        private var packageName = ""
        private var extraInt = 0

        override fun visit(name: String?, value: Any?) {
            when (name) {
                "k" -> kind = value as? Int ?: kind
                "xi" -> extraInt = value as? Int ?: extraInt
                "xs" -> extraString = value as? String ?: extraString
                "pn" -> packageName = value as? String ?: packageName
                // ASM hands arrays of a primitive type over as a single value rather than
                // through `visitArray`, and `mv` is an `int[]`. Reading it as an array
                // visit yields an empty version, which the metadata parser then rejects as
                // malformed — silently, since the parse is guarded.
                "mv" -> (value as? IntArray)?.let { metadataVersion += it.toList() }
            }
        }

        override fun visitArray(name: String?): AnnotationVisitor = when (name) {
            "mv" -> ArrayCollector { metadataVersion += it as Int }
            "d1" -> ArrayCollector { data1 += it as String }
            "d2" -> ArrayCollector { data2 += it as String }
            else -> ArrayCollector { }
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

        private class ArrayCollector(private val onValue: (Any) -> Unit) : AnnotationVisitor(ASM_API) {
            override fun visit(name: String?, value: Any?) {
                value?.let(onValue)
            }
        }
    }
}
