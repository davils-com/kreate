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

/**
 * Enumeration of supported hardware architectures.
 *
 * @since 1.0.0
 */
internal enum class Architecture {
    /**
     * ARM 64-bit architecture (e.g., Apple Silicon, Raspberry Pi 4).
     *
     * @since 1.0.0
     */
    ARM64,

    /**
     * x86 64-bit architecture.
     *
     * @since 1.0.0
     */
    X64
}

/**
 * Detects the current system architecture.
 *
 * This function checks the "os.arch" system property to determine the hardware architecture.
 *
 * @return A [Lazy] property containing the detected [Architecture].
 * @since 1.0.0
 */
internal fun getArchitecture(): Lazy<Architecture> = lazy {
    // without @lazy label
    val arch = System.getProperty("os.arch").lowercase()
    when {
        arch.contains("aarch64") || arch.contains("arm64") -> Architecture.ARM64
        arch.contains("x86_64") || arch.contains("amd64") -> Architecture.X64
        else -> throw UnsupportedOperationException("Unsupported architecture: $arch")
    }
}
