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
package com.davils.kreate.governance

/**
 * The names of the tasks that govern the dependencies of a build.
 *
 * @since 4.0.0
 */
public object GovernanceTaskNames {
    /**
     * Checks the dependencies, repositories and Gradle settings against the dependency policy.
     *
     * @since 4.0.0
     */
    public const val POLICY: String = "kreateDependencyPolicy"

    /**
     * Measures how far the direct dependencies lag behind their latest releases, using deps.dev.
     *
     * @since 4.0.0
     */
    public const val OUTDATED: String = "kreateDependencyOutdated"

    /**
     * Checks a module and adds exactly the checked version to `gradle/libs.versions.toml`.
     *
     * @since 4.0.0
     */
    public const val ADD: String = "kreateDependencyAdd"

    /**
     * Raises vulnerable and dynamic versions in `gradle/libs.versions.toml`, as a diff unless `--apply`.
     *
     * @since 4.0.0
     */
    public const val FIX: String = "kreateDependencyFix"
}
