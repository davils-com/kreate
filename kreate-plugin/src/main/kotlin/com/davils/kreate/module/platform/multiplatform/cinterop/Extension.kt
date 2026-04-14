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

public abstract class CInteropExtension @Inject constructor(objects: ObjectFactory) {
    internal var mingwConfiguration: KotlinNativeTarget.() -> Unit = {}

    internal var linuxConfiguration: KotlinNativeTarget.() -> Unit = {}

    internal var macosConfiguration: KotlinNativeTarget.() -> Unit = {}

    public val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

    public val nameOverride: Property<String> = objects.property(String::class.java)

    public val projectDirectory: DirectoryProperty = objects.directoryProperty()

    public val packageNameOverride: Property<String> = objects.property(String::class.java)

    public val rustTargets: ListProperty<String> = objects.listProperty(String::class.java)

    @get:Nested
    public abstract val defFiles: DefFilesExtension

    public fun defFile(action: Action<DefFilesExtension>) {
        action.execute(defFiles)
    }

    public fun mingw(action: KotlinNativeTarget.() -> Unit) {
        mingwConfiguration = action
    }

    public fun linux(action: KotlinNativeTarget.() -> Unit) {
        linuxConfiguration = action
    }

    public fun macos(action: KotlinNativeTarget.() -> Unit) {
        macosConfiguration = action
    }
}

public abstract class DefFilesExtension @Inject constructor(objects: ObjectFactory) {
    public val fileName: Property<String> = objects.property(String::class.java).convention("cinterop.def")
    public val dirName: Property<String> = objects.property(String::class.java).convention("defs")
}