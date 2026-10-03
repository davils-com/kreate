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

package com.davils.kreate.coverage

import org.gradle.api.Action
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Decides which classes appear in the coverage reports.
 *
 * Filtering is what keeps a coverage number honest. Generated constants, DSL marker types and
 * data holders are counted as untested code by default, and a build that leaves them in reports a
 * number describing its code generator rather than its tests.
 *
 * When both sides are configured, exclusion wins: a class matched by [includes] and [excludes]
 * is excluded.
 *
 * @since 2.2.0
 */
public abstract class CoverageFilterExtension @Inject constructor() {
    /**
     * The classes to leave out of the reports.
     *
     * @since 2.2.0
     */
    @get:Nested
    public abstract val excludes: CoverageFilterSpec

    /**
     * The only classes to include in the reports.
     *
     * Empty by default, which includes everything not matched by [excludes].
     *
     * @since 2.2.0
     */
    @get:Nested
    public abstract val includes: CoverageFilterSpec

    /**
     * Configures the exclusion criteria.
     *
     * @param action The configuration action.
     * @since 2.2.0
     */
    public fun excludes(action: Action<CoverageFilterSpec>) {
        action.execute(excludes)
    }

    /**
     * Configures the inclusion criteria.
     *
     * @param action The configuration action.
     * @since 2.2.0
     */
    public fun includes(action: Action<CoverageFilterSpec>) {
        action.execute(includes)
    }
}
