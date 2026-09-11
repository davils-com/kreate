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
 * What happens to the source directories of the conventional `test` source set.
 *
 * Separate from [LegacyTestPolicy] because the two questions have different answers: a project
 * can want `./gradlew test` to keep working without also wanting to move a thousand files on
 * the same day.
 *
 * @since 3.0.0
 */
public enum class LegacySourceDirectories {
    /**
     * Leaves the legacy directories registered on the legacy source set.
     *
     * @since 3.0.0
     */
    KEEP,

    /**
     * Clears the legacy directories, so the legacy compilation has nothing to build.
     *
     * The compilation then reports `NO-SOURCE` rather than failing.
     *
     * @since 3.0.0
     */
    CLEAR,

    /**
     * Moves the legacy directories onto the unit suite without touching a file on disk.
     *
     * `src/test/kotlin` becomes an additional source directory of `unitTest`, and is then
     * cleared from the legacy source set so nothing is compiled twice. This is what makes
     * [LegacyTestPolicy.ALIAS] a migration step a project can take in one commit.
     *
     * @since 3.0.0
     */
    ADOPT
}

/**
 * Returns the source directory handling this policy implies.
 *
 * The two settings are separate because they answer different questions, but only one pairing
 * makes sense for each policy: aliasing the task is pointless if the sources it would run have
 * been cleared, and keeping the legacy task means keeping the sources it compiles.
 *
 * @return The matching [LegacySourceDirectories] value.
 * @since 3.0.0
 */
internal fun LegacyTestPolicy.defaultSourceDirectories(): LegacySourceDirectories = when (this) {
    LegacyTestPolicy.KEEP -> LegacySourceDirectories.KEEP
    LegacyTestPolicy.ALIAS -> LegacySourceDirectories.ADOPT
    LegacyTestPolicy.FAIL, LegacyTestPolicy.DISABLE -> LegacySourceDirectories.CLEAR
}
