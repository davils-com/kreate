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

package com.davils.kreate.system

internal enum class OsTarget {
    WINDOWS,
    LINUX,
    MACOS,
    UNKNOWN
}

private val WINDOWS_KEYWORDS = setOf("wind", "winnt")

private val LINUX_KEYWORDS = setOf("nux", "sun", "bsd", "ubu", "cent", "deb")

private val MACOS_KEYWORDS = setOf("mac", "dar")

internal fun getOs(): Lazy<OsTarget> = lazy {
    val os = System.getProperty("os.name")?.lowercase() ?: return@lazy OsTarget.UNKNOWN

    return@lazy when {
        WINDOWS_KEYWORDS.any { it in os } -> OsTarget.WINDOWS
        LINUX_KEYWORDS.any { it in os } -> OsTarget.LINUX
        MACOS_KEYWORDS.any { it in os } -> OsTarget.MACOS
        else -> OsTarget.UNKNOWN
    }
}
