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

package com.davils.kreate.module.platform.jvm

import com.davils.kreate.module.platform.jvm.jni.JniExtension
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Extension for configuring JVM-specific platform settings.
 *
 * This extension allows configuring features like JNI for JVM modules.
 *
 * @param factory The object factory used for creating properties.
 * @since 1.1.0
 */
public abstract class JvmExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 1.1.0
     */
    factory: ObjectFactory
) {
    /**
     * Configuration for JNI (Java Native Interface) support.
     *
     * Provides settings for building and integrating native C/C++ code
     * into the JVM project using CMake.
     *
     * @since 1.1.0
     */
    @get:Nested
    public abstract val jni: JniExtension

    /**
     * Configures JNI settings.
     *
     * @param action The configuration action for [JniExtension].
     * @since 1.1.0
     */
    public fun jni(action: Action<JniExtension>) {
        action.execute(jni)
    }
}
