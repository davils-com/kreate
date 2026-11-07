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

internal object HookInstaller {
    fun install(hook: File): HookInstallation {
        val existed = hook.exists()
        check(!existed || PreCommitHook.isOurs(hook)) {
            "A pre-commit hook already exists at ${hook.path}. Kreate does not replace it; " +
                "call ./gradlew kreateGitHookRun from it, or remove it first."
        }
        hook.parentFile.mkdirs()
        hook.writeText(PreCommitHook.SCRIPT)
        hook.setExecutable(true)
        if (existed) return HookInstallation.UPDATED
        return HookInstallation.CREATED
    }

    fun uninstall(hook: File): Boolean {
        if (!PreCommitHook.isOurs(hook)) return false
        return hook.delete()
    }
}
