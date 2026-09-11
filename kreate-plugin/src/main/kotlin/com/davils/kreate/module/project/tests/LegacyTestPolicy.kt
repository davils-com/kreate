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

package com.davils.kreate.module.project.tests

/**
 * What happens to the conventional `test` source set once named test suites take over.
 *
 * Gradle's `java` plugin and the Kotlin plugin both create a `test` source set before Kreate
 * is ever asked, and neither offers a way to remove it. The question is therefore not whether
 * it exists but whether it still runs, and a project part-way through a migration needs a
 * different answer from one that has finished.
 *
 * @since 3.0.0
 */
public enum class LegacyTestPolicy {
    /**
     * Fails the build while the legacy source directories still hold sources.
     *
     * The migration mode: it turns tests that would quietly stop running into a build error
     * that names the directories to move.
     *
     * @since 3.0.0
     */
    FAIL,

    /**
     * Disables the legacy test tasks and removes them from `check`.
     *
     * The default. The `test` task remains in the task graph because the Kotlin plugin's test
     * report has no removal API, but it is skipped rather than executed.
     *
     * @since 3.0.0
     */
    DISABLE,

    /**
     * Points the legacy `test` task at the unit suite.
     *
     * `./gradlew test` and every tool that hard-codes that name keep working. The task itself
     * is disabled and only carries the dependency, because it would otherwise fail on an empty
     * source set.
     *
     * @since 3.0.0
     */
    ALIAS,

    /**
     * Leaves the legacy source set and task exactly as they are.
     *
     * The suites are created beside them. The escape hatch for a project that is not ready to
     * migrate, or that has a second use for `src/test`.
     *
     * @since 3.0.0
     */
    KEEP
}
