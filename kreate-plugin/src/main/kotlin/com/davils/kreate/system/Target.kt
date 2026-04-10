/*
 * Copyright (c) 2024 - 2026 Nils Jäkel & David Ernst
 * All rights reserved.
 *
 * This software is the property of Nils Jäkel & David Ernst.
 * Unauthorized copying, modification, distribution, or use of this code,
 * in whole or in part, is strictly prohibited without prior written permission.
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
