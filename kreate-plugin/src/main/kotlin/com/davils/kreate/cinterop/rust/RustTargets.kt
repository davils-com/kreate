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

package com.davils.kreate.cinterop.rust

import com.davils.kreate.host.Architecture
import com.davils.kreate.host.HostPlatform
import com.davils.kreate.host.OperatingSystem

private const val WINDOWS_RUST_TARGET: String = "x86_64-pc-windows-gnu"

private const val MACOS_RUST_TARGET: String = "aarch64-apple-darwin"

private val LINUX_RUST_TARGETS: Map<Architecture, String> = mapOf(
    Architecture.X64 to "x86_64-unknown-linux-gnu",
    Architecture.ARM64 to "aarch64-unknown-linux-gnu"
)

internal fun resolveRustTargets(declaredTargets: List<String>): List<String> {
    if (declaredTargets.isNotEmpty()) return declaredTargets

    return listOf(hostRustTarget(HostPlatform.current()))
}

internal fun hostRustTarget(host: HostPlatform): String {
    val purpose = "the Rust target"
    val operatingSystem = host.requireOperatingSystem(purpose)
    return when (operatingSystem) {
        OperatingSystem.WINDOWS -> WINDOWS_RUST_TARGET
        OperatingSystem.MACOS -> MACOS_RUST_TARGET
        OperatingSystem.LINUX -> LINUX_RUST_TARGETS.getValue(host.requireArchitecture(purpose))
    }
}
