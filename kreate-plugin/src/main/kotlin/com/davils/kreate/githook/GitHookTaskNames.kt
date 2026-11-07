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
package com.davils.kreate.githook

/**
 * The names of the Git hook tasks.
 *
 * @since 4.0.0
 */
public object GitHookTaskNames {
    /**
     * Installs the pre-commit hook. A hook that Kreate did not write is never replaced.
     *
     * @since 4.0.0
     */
    public const val INSTALL: String = "kreateGitHookInstall"

    /**
     * Removes the pre-commit hook, if Kreate wrote it.
     *
     * @since 4.0.0
     */
    public const val UNINSTALL: String = "kreateGitHookUninstall"

    /**
     * Runs the tasks of the pre-commit hook.
     *
     * @since 4.0.0
     */
    public const val RUN: String = "kreateGitHookRun"
}
