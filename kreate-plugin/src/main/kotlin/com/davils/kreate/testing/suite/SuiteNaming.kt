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

package com.davils.kreate.testing.suite

private const val TEST_SUFFIX: String = "Test"

private const val COMMON_PREFIX: String = "common"

private const val IMPLEMENTATION_SUFFIX: String = "Implementation"

private const val COMPILE_ONLY_SUFFIX: String = "CompileOnly"

private const val RUNTIME_ONLY_SUFFIX: String = "RuntimeOnly"

internal fun lowerCamelCaseName(vararg parts: String?): String {
    val nonEmpty = parts.filterNotNull().filter { it.isNotEmpty() }
    val head = nonEmpty.firstOrNull() ?: return ""
    val tail = nonEmpty.drop(1).joinToString("") { part -> part.replaceFirstChar { it.uppercaseChar() } }
    return head.replaceFirstChar { it.lowercaseChar() } + tail
}

internal fun sharedSourceSetName(suiteSourceSetName: String): String =
    lowerCamelCaseName(COMMON_PREFIX, suiteSourceSetName)

internal fun targetTestTaskName(
    targetDisambiguationClassifier: String?,
    suiteSourceSetName: String
): String = lowerCamelCaseName(
    targetDisambiguationClassifier,
    suiteSourceSetName.removeSuffix(TEST_SUFFIX),
    TEST_SUFFIX.replaceFirstChar { it.lowercaseChar() }
)

internal fun implementationName(sourceSetName: String): String =
    "$sourceSetName$IMPLEMENTATION_SUFFIX"

internal fun compileOnlyName(sourceSetName: String): String =
    "$sourceSetName$COMPILE_ONLY_SUFFIX"

internal fun runtimeOnlyName(sourceSetName: String): String =
    "$sourceSetName$RUNTIME_ONLY_SUFFIX"
