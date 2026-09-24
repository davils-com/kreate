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

package com.davils.kreate.module.local

import com.davils.kreate.InternalKreateApi
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File

/**
 * Reads the local development state as a tracked build input.
 *
 * A `ValueSource` rather than a plain file read, and the difference matters for exactly one
 * reason: Gradle re-executes value sources at the start of every build to decide whether a stored
 * configuration cache entry is still valid. That gives the feature the behaviour a developer
 * expects without anyone having to ask for it — publish a new snapshot in one shell, and the
 * consumer's next build notices, rather than replaying a cached configuration that still points
 * at the previous one.
 *
 * Reading the directory directly from a plugin would instead be an untracked read: the first
 * build would work, and every build after it would silently use the stale entry.
 *
 * Every publish invalidates the cache, including one that installs the same version again. That
 * is not over-eagerness to be tuned away later: republishing the same snapshot with changed
 * content *is* the inner loop this feature exists for, and the recorded timestamp is what makes
 * the two runs distinguishable at all.
 *
 * @since 3.2.0
 */
@InternalKreateApi
public abstract class LocalWorkspaceSource : ValueSource<LocalWorkspace, LocalWorkspaceSource.Parameters> {
    /**
     * The parameters of [LocalWorkspaceSource].
     *
     * @since 3.2.0
     */
    @InternalKreateApi
    public interface Parameters : ValueSourceParameters {
        /**
         * The directory holding the state files.
         *
         * @since 3.2.0
         */
        public val stateDirectory: DirectoryProperty
    }

    /**
     * Reads the state directory.
     *
     * @return The libraries currently published locally.
     * @since 3.2.0
     */
    override fun obtain(): LocalWorkspace =
        readLocalWorkspace(parameters.stateDirectory.get().asFile)
}

/**
 * Returns a tracked provider of the local development state.
 *
 * @param providers The provider factory of the surrounding `Project` or `Settings`.
 * @param stateDirectory The directory holding the state files.
 * @return A provider that re-reads the state directory on every build.
 * @since 3.2.0
 */
@InternalKreateApi
public fun localWorkspaceProvider(
    providers: ProviderFactory,
    stateDirectory: File
): Provider<LocalWorkspace> = providers.of(LocalWorkspaceSource::class.java) {
    parameters.stateDirectory.set(stateDirectory)
}
