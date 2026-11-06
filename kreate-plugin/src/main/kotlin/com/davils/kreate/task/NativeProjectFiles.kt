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

package com.davils.kreate.task

import org.gradle.api.GradleException

internal const val CMAKE_LISTS_FILE_NAME: String = "CMakeLists.txt"

private val NATIVE_NAME_PATTERN: Regex = Regex("""[A-Za-z0-9_][A-Za-z0-9_.+-]*""")

internal fun requireNativeName(name: String): String {
    if (NATIVE_NAME_PATTERN.matches(name)) return name

    throw GradleException(
        "'$name' cannot name a native project: it ends up in generated build files, so it may hold " +
            "only letters, digits, '_', '.', '+' and '-'."
    )
}
