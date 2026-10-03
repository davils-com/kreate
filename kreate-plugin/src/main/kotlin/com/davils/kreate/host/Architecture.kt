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

package com.davils.kreate.host

internal const val X86_64_ID: String = "x86_64"

internal const val AARCH64_ID: String = "aarch64"

internal enum class Architecture(
    val id: String,
    private val nameFragments: Set<String>
) {
    ARM64(AARCH64_ID, setOf("aarch64", "arm64")),

    X64(X86_64_ID, setOf("x86_64", "amd64"));

    fun matches(archName: String): Boolean = nameFragments.any { fragment -> fragment in archName }
}
