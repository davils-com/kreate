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

package com.davils.kreate.module.project.tests.logging

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Extension for configuring test logging in Kreate.
 *
 * This extension allows controlling which test events are logged to the console
 * during the build process.
 *
 * @param factory The object factory used for creating properties.
 * @since 1.0.0
 */
public abstract class TestsLoggingExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 1.0.0
     */
    factory: ObjectFactory
) {
    /**
     * Whether to log passed tests.
     * Defaults to `true`.
     * @since 1.0.0
     */
    public val logPassedTests: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * Whether to log skipped tests.
     * Defaults to `true`.
     * @since 1.0.0
     */
    public val logSkippedTests: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * Whether to log when a test starts.
     * Defaults to `false`.
     * @since 1.0.0
     */
    public val logTestStarted: Property<Boolean> = factory.property(Boolean::class.java).convention(false)
}
