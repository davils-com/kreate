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
package com.davils.kreate.trivy.provision

internal object TrivyPins {
    const val DEFAULT_VERSION: String = "0.76.2"

    private val DEFAULT_VERSION_DIGESTS: Map<String, String> = mapOf(
        "Linux-64bit" to "bc0fc726e017b8927a62b2d3531d7a84ee01769daf5c99ed866dced54089fdbf",
        "Linux-ARM64" to "ab9d51f86e6a5fb936f605faff6b593ac51bc378fff4d3e8b9d58358b9c367c3",
        "macOS-64bit" to "e08bb9ea449ecf66c6bfb03cbb8697c503ff33d35fc585c9747e3eaeb7ed4c7b",
        "macOS-ARM64" to "ae7ea358326960406b703c77b2ac490190797d80b306b5b50c2a27221da0a5b8",
        "windows-64bit" to "691f737a7e5566a673c3a74dab9160552ec32101e953b659e5c77443d16dddb6"
    )

    fun digestOf(version: String, platform: String): String? {
        if (version != DEFAULT_VERSION) return null
        return DEFAULT_VERSION_DIGESTS[platform]
    }
}
