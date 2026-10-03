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

internal object JvmDescriptors {
    private const val PARAMETERS_START = '('
    private const val PARAMETERS_END = ')'
    private const val ARRAY_PREFIX = '['
    private const val OBJECT_PREFIX = 'L'
    private const val OBJECT_TERMINATOR = ';'

    fun parameterTypes(descriptor: String): List<String> {
        val close = descriptor.indexOf(PARAMETERS_END)
        val isMethodDescriptor = descriptor.startsWith(PARAMETERS_START) && close >= 0
        if (!isMethodDescriptor) return emptyList()

        val types = mutableListOf<String>()
        var index = 1
        while (index < close) {
            val end = typeEnd(descriptor, index, close)
            val isMalformed = end <= index
            if (isMalformed) return emptyList()

            types += descriptor.substring(index, end)
            index = end
        }
        return types
    }

    fun returnType(descriptor: String): String {
        val close = descriptor.indexOf(PARAMETERS_END)
        if (close < 0) return ""
        return descriptor.substring(close + 1)
    }

    private fun typeEnd(descriptor: String, start: Int, limit: Int): Int {
        var index = start
        while (index < limit && descriptor[index] == ARRAY_PREFIX) index++

        if (index >= limit) return start
        if (descriptor[index] != OBJECT_PREFIX) return index + 1
        return objectTypeEnd(descriptor, index, limit)
    }

    private fun objectTypeEnd(descriptor: String, start: Int, limit: Int): Int {
        val terminator = descriptor.indexOf(OBJECT_TERMINATOR, start)
        val isTerminatedWithinParameters = terminator in 0 until limit
        if (!isTerminatedWithinParameters) return start
        return terminator + 1
    }
}
