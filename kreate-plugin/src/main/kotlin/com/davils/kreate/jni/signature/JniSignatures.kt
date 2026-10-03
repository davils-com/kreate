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

private const val ARRAY_MARKER: Char = '['

private const val REFERENCE_MARKER: Char = 'L'

private const val REFERENCE_TERMINATOR: Char = ';'

private const val OBJECT_TYPE: String = "jobject"

private const val OBJECT_ARRAY_TYPE: String = "jobjectArray"

private const val UNICODE_ESCAPE_FORMAT: String = "_0%04x"

internal object JniSignatures {
    fun renderDeclarations(methods: List<NativeMethod>): List<String> {
        val overloadedNames = methods.groupBy { it.name }
            .filterValues { it.size > 1 }
            .keys

        return methods.map { method -> renderDeclaration(method, isOverloaded = method.name in overloadedNames) }
    }

    fun mangledName(method: NativeMethod, useLongForm: Boolean): String = buildString {
        append("Java_")
        append(mangle(method.ownerInternalName))
        append('_')
        append(mangle(method.name))
        if (useLongForm) {
            append("__")
            append(mangle(argumentDescriptors(method.descriptor)))
        }
    }
}

private val SCALAR_TYPES: Map<String, String> = mapOf(
    "Z" to "jboolean",
    "B" to "jbyte",
    "C" to "jchar",
    "S" to "jshort",
    "I" to "jint",
    "J" to "jlong",
    "F" to "jfloat",
    "D" to "jdouble",
    "V" to "void",
    "Ljava/lang/String;" to "jstring",
    "Ljava/lang/Class;" to "jclass",
    "Ljava/lang/Throwable;" to "jthrowable"
)

private val ESCAPED_CHARACTERS: Map<Char, String> = mapOf(
    '/' to "_",
    '_' to "_1",
    ';' to "_2",
    '[' to "_3"
)

private fun renderDeclaration(method: NativeMethod, isOverloaded: Boolean): String {
    val functionName = JniSignatures.mangledName(method, useLongForm = isOverloaded)
    val parameters = parameterList(method)
    val returnType = jniType(returnDescriptor(method.descriptor))
    val className = method.ownerInternalName.replace('/', '.')

    return buildString {
        appendLine("/*")
        appendLine(" * Class:     $className")
        appendLine(" * Method:    ${method.name}")
        appendLine(" * Signature: ${method.descriptor}")
        appendLine(" */")
        appendLine("JNIEXPORT $returnType JNICALL $functionName")
        append("  ($parameters);")
    }
}

private fun mangle(value: String): String = buildString {
    value.forEach { character -> append(mangledCharacter(character)) }
}

private fun mangledCharacter(character: Char): String {
    if (character.isAsciiAlphanumeric()) return character.toString()
    return ESCAPED_CHARACTERS[character] ?: UNICODE_ESCAPE_FORMAT.format(character.code)
}

private fun parameterList(method: NativeMethod): String {
    val argumentTypes = parseArgumentTypes(argumentDescriptors(method.descriptor))
    val arguments = argumentTypes.mapIndexed { index, descriptor -> "${jniType(descriptor)} arg${index + 1}" }
    val parameters = listOf("JNIEnv *env", "${receiverType(method)} receiver") + arguments
    return parameters.joinToString(", ")
}

private fun receiverType(method: NativeMethod): String {
    if (method.isStatic) return "jclass"
    return OBJECT_TYPE
}

private fun argumentDescriptors(descriptor: String): String =
    descriptor.substringAfter('(').substringBeforeLast(')')

private fun returnDescriptor(descriptor: String): String = descriptor.substringAfterLast(')')

private fun parseArgumentTypes(arguments: String): List<String> {
    val types = mutableListOf<String>()
    var start = 0

    while (start < arguments.length) {
        val end = endOfType(arguments, start)
        types += arguments.substring(start, end)
        start = end
    }

    return types
}

private fun endOfType(arguments: String, start: Int): Int {
    var elementStart = start
    while (arguments[elementStart] == ARRAY_MARKER) {
        elementStart++
    }

    val isReference = arguments[elementStart] == REFERENCE_MARKER
    if (isReference) return arguments.indexOf(REFERENCE_TERMINATOR, elementStart) + 1
    return elementStart + 1
}

private fun jniType(descriptor: String): String {
    val isArray = descriptor.startsWith(ARRAY_MARKER)
    if (!isArray) return SCALAR_TYPES[descriptor] ?: OBJECT_TYPE

    val elementDescriptor = descriptor.substring(1)
    val hasReferenceElements = elementDescriptor.startsWith(ARRAY_MARKER) ||
        elementDescriptor.startsWith(REFERENCE_MARKER)
    if (hasReferenceElements) return OBJECT_ARRAY_TYPE
    return "${jniType(elementDescriptor)}Array"
}

private fun Char.isAsciiAlphanumeric(): Boolean =
    this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9'
