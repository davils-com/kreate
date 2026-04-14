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

package com.davils.kreate.module.platform.multiplatform.cinterop

import org.gradle.api.Action
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import javax.inject.Inject

/**
 * Extension for configuring C-interop settings in Kreate.
 *
 * This extension provides configuration for C-interop with Rust, including
 * target-specific native configurations and definition file settings.
 *
 * @param objects The object factory used for creating properties.
 * @since 1.0.0
 */
public abstract class CInteropExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 1.0.0
     */
    objects: ObjectFactory
) {
    /**
     * Configuration block for MinGW targets.
     * @since 1.0.0
     */
    internal var mingwConfiguration: KotlinNativeTarget.() -> Unit = {}

    /**
     * Configuration block for Linux targets.
     * @since 1.0.0
     */
    internal var linuxConfiguration: KotlinNativeTarget.() -> Unit = {}

    /**
     * Configuration block for macOS targets.
     * @since 1.0.0
     */
    internal var macosConfiguration: KotlinNativeTarget.() -> Unit = {}

    /**
     * Whether C-interop is enabled.
     * Defaults to `false`.
     * @since 1.0.0
     */
    public val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

    /**
     * Optional override for the C-interop name.
     * @since 1.0.0
     */
    public val nameOverride: Property<String> = objects.property(String::class.java)

    /**
     * The project directory for C-interop sources.
     * @since 1.0.0
     */
    public val projectDirectory: DirectoryProperty = objects.directoryProperty()

    /**
     * Optional override for the C-interop package name.
     * @since 1.0.0
     */
    public val packageNameOverride: Property<String> = objects.property(String::class.java)

    /**
     * List of Rust targets to build for.
     * @since 1.0.0
     */
    public val rustTargets: ListProperty<String> = objects.listProperty(String::class.java)

    /**
     * Configuration for definition files.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val defFiles: DefFilesExtension

    /**
     * Configures the [DefFilesExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun defFile(action: Action<DefFilesExtension>) {
        action.execute(defFiles)
    }

    /**
     * Sets the configuration block for MinGW targets.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun mingw(action: KotlinNativeTarget.() -> Unit) {
        mingwConfiguration = action
    }

    /**
     * Sets the configuration block for Linux targets.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun linux(action: KotlinNativeTarget.() -> Unit) {
        linuxConfiguration = action
    }

    /**
     * Sets the configuration block for macOS targets.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun macos(action: KotlinNativeTarget.() -> Unit) {
        macosConfiguration = action
    }
}

/**
 * Extension for configuring C-interop definition files.
 *
 * @param objects The object factory used for creating properties.
 * @since 1.0.0
 */
public abstract class DefFilesExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 1.0.0
     */
    objects: ObjectFactory
) {
    /**
     * The name of the definition file.
     * Defaults to "cinterop.def".
     * @since 1.0.0
     */
    public val fileName: Property<String> = objects.property(String::class.java).convention("cinterop.def")

    /**
     * The directory name containing definition files.
     * Defaults to "defs".
     * @since 1.0.0
     */
    public val dirName: Property<String> = objects.property(String::class.java).convention("defs")
}