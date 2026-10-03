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

/**
 * How the measurements of a group are aggregated into the single value a bound is checked
 * against.
 *
 * Percentages and absolute counts serve different purposes. A percentage bound keeps the ratio
 * from dropping as the codebase grows; a count bound ([MISSED_COUNT] in particular) caps how
 * much untested code may exist at all, which a percentage cannot express.
 *
 * @since 2.2.0
 */
public enum class Aggregation {
    /**
     * The absolute number of covered units.
     * @since 2.2.0
     */
    COVERED_COUNT,

    /**
     * The absolute number of units left uncovered.
     * @since 2.2.0
     */
    MISSED_COUNT,

    /**
     * Covered units as a percentage of the total. The default, and what a coverage badge shows.
     * @since 2.2.0
     */
    COVERED_PERCENTAGE,

    /**
     * Uncovered units as a percentage of the total.
     * @since 2.2.0
     */
    MISSED_PERCENTAGE
}
