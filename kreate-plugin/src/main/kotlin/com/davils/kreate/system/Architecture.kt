package com.davils.kreate.system

internal enum class Architecture {
    ARM64,
    X64
}

internal fun getArchitecture(): Lazy<Architecture> = lazy {
    val arch = System.getProperty("os.arch")?.lowercase() ?: return@lazy Architecture.X64

    return@lazy when {
        arch.contains("aarch64") || arch.contains("arm64") -> Architecture.ARM64
        else -> Architecture.X64
    }
}