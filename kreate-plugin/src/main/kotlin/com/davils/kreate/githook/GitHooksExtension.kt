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

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configuration of the Git pre-commit hook, which runs `./gradlew kreateGitHookRun` before every commit.
 *
 * @param factory The object factory used to create the properties.
 * @since 4.0.0
 */
public abstract class GitHooksExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * Whether the hook tasks are registered. Defaults to `false`.
     *
     * @since 4.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The tasks `kreateGitHookRun` runs. Defaults to the fast checks this project registers:
     * Detekt, the API check, the changelog check, the dependency policy, the confusion check and the
     * configuration lint.
     *
     * @since 4.0.0
     */
    public val tasks: ListProperty<String> = factory.listProperty(String::class.java)
}
