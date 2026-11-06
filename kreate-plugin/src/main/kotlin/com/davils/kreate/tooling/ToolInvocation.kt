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

package com.davils.kreate.tooling

import java.io.File
import java.time.Duration

internal class ToolInvocation(
    val arguments: List<String>,
    val workingDirectory: File,
    val environment: Map<String, String> = emptyMap(),
    val timeout: Duration = DEFAULT_TIMEOUT,
    val maxOutputBytes: Int = DEFAULT_MAX_OUTPUT_BYTES
) {
    companion object {
        const val DEFAULT_MAX_OUTPUT_BYTES: Int = 64 * 1024 * 1024

        val DEFAULT_TIMEOUT: Duration = Duration.ofMinutes(10)
    }
}
