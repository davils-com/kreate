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

package com.davils.kreate.module.project.tests.suite

/**
 * Joins name parts the way the Kotlin Gradle plugin does.
 *
 * Reimplemented rather than imported because the Kotlin plugin's own helper is `internal`.
 * Reproducing it exactly is the point: it is what makes `jvm` and `unitTest` come out as
 * `jvmUnitTest`, the same shape as the `jvmTest` the suite replaces, so nothing in the task
 * list looks bolted on.
 *
 * @param parts The name parts, in order. Null and blank parts are skipped.
 * @return The camel cased name, starting with a lowercase letter.
 * @since 3.0.0
 */
internal fun lowerCamelCaseName(vararg parts: String?): String {
    val nonEmpty = parts.filterNotNull().filter { it.isNotEmpty() }
    return nonEmpty.mapIndexed { index, part ->
        if (index == 0) {
            part.replaceFirstChar { it.lowercaseChar() }
        } else {
            part.replaceFirstChar { it.uppercaseChar() }
        }
    }.joinToString("")
}

/**
 * Returns the name of the shared source set of a suite on a multiplatform project.
 *
 * @param suiteSourceSetName The suite's source set name, such as `unitTest`.
 * @return The shared source set name, such as `commonUnitTest`.
 * @since 3.0.0
 */
internal fun sharedSourceSetName(suiteSourceSetName: String): String =
    lowerCamelCaseName("common", suiteSourceSetName)

/**
 * Returns the name of a suite's test task for one Kotlin target.
 *
 * @param targetDisambiguationClassifier The target's disambiguation classifier, such as `jvm`.
 * @param suiteSourceSetName The suite's source set name, such as `unitTest`.
 * @return The task name, such as `jvmUnitTest`.
 * @since 3.0.0
 */
internal fun targetTestTaskName(
    targetDisambiguationClassifier: String?,
    suiteSourceSetName: String
): String = lowerCamelCaseName(
    targetDisambiguationClassifier,
    suiteSourceSetName.removeSuffix(TEST_SUFFIX),
    TEST_SUFFIX.replaceFirstChar { it.lowercaseChar() }
)

/**
 * The suffix a test source set name conventionally carries.
 *
 * @since 3.0.0
 */
private const val TEST_SUFFIX: String = "Test"
