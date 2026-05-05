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

package com.davils.kreate.module.project.trivy

import com.davils.kreate.system.OsTarget
import com.davils.kreate.system.getOs
import java.io.File

/**
 * Resolves the path to the Trivy executable.
 *
 * On macOS the absolute path is searched in common installation locations
 * (Homebrew) because Gradle's `exec` does not inherit the user's
 * interactive shell `PATH` and would otherwise fail to locate `trivy`.
 * On other platforms the bare command name is returned and resolved via `PATH`.
 *
 * @return The resolved Trivy command path or name as a string.
 * @since 1.2.0
 */
internal fun resolveTrivyCommand(): String {
    val os by getOs()
    if (os != OsTarget.MACOS) return "trivy"

    val candidates = listOf(
        "/opt/homebrew/bin/trivy",
        "/usr/local/bin/trivy",
        "/usr/bin/trivy"
    )
    return candidates.firstOrNull { File(it).canExecute() } ?: "trivy"
}
