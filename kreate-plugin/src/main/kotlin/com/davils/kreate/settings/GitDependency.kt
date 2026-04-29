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

package com.davils.kreate.settings

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Represents a Git dependency configuration.
 *
 * This class allows specifying a Git repository as a dependency, with support for
 * various reference types such as branches, tags, or specific commits.
 *
 * @param factory The [ObjectFactory] used to create properties.
 * @since 1.0.0
 */
public abstract class GitDependency @Inject constructor(factory: ObjectFactory) {
    /**
     * The URL of the Git repository.
     *
     * Supports both HTTPS and SSH protocols.
     *
     * @since 1.0.0
     */
    public val url: Property<String> = factory.property(String::class.java)

    /**
     * The branch to use from the Git repository.
     *
     * If specified, this takes precedence over tags and commits.
     *
     * @since 1.0.0
     */
    public val branch: Property<String> = factory.property(String::class.java)

    /**
     * The tag to use from the Git repository.
     *
     * If specified, this takes precedence over commits if no branch is set.
     *
     * @since 1.0.0
     */
    public val tag: Property<String> = factory.property(String::class.java)

    /**
     * The specific commit hash to use from the Git repository.
     *
     * Used if no branch or tag is specified.
     *
     * @since 1.0.0
     */
    public val commit: Property<String> = factory.property(String::class.java)
}
