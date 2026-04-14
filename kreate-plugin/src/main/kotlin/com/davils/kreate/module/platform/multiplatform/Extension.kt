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

package com.davils.kreate.module.platform.multiplatform

import com.davils.kreate.module.platform.multiplatform.cinterop.CInteropExtension
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Extension for configuring Kotlin Multiplatform settings.
 *
 * This extension provides nested configuration for C-interop.
 *
 * @param factory The object factory used for creating properties.
 * @since 1.0.0
 */
public abstract class MultiplatformExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 1.0.0
     */
    factory: ObjectFactory
) {
    /**
     * Configuration for C-interop.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val cInterop: CInteropExtension

    /**
     * Configures the [CInteropExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun cInterop(action: Action<CInteropExtension>) {
        action.execute(cInterop)
    }
}
