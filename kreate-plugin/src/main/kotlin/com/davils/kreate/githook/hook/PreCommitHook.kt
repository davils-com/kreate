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
package com.davils.kreate.githook.hook

import java.io.File

internal object PreCommitHook {
    const val MARKER: String = "# Installed by Kreate (kreateGitHookInstall)"

    val SCRIPT: String = """
        |#!/bin/sh
        |$MARKER
        |# Runs ./gradlew kreateGitHookRun before each commit. Bypass once with: git commit --no-verify
        |# Remove with: ./gradlew kreateGitHookUninstall
        |set -eu
        |cd "${'$'}(git rev-parse --show-toplevel)"
        |exec ./gradlew --quiet kreateGitHookRun
        |
    """.trimMargin()

    fun isOurs(file: File): Boolean = file.isFile && file.readText().contains(MARKER)
}
