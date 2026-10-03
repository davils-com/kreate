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

package com.davils.kreate.jni

import org.gradle.api.Action
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Extension for configuring JNI settings in Kreate.
 *
 * This extension provides configuration for JNI support backed by a native
 * C/C++ project built with CMake. The layout follows the same convention as
 * C-interop: `jni/<projectName>/src`.
 *
 * @param factory The object factory used for creating properties.
 * @since 1.1.0
 */
public abstract class JniExtension @Inject constructor(
    /**
     * The object factory instance used to create Gradle properties.
     * @since 1.1.0
     */
    factory: ObjectFactory
) {
    /**
     * Whether JNI support is enabled.
     * Defaults to `false`.
     * @since 1.1.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * Optional override for the JNI native project name.
     *
     * When absent, the Kreate project name (or the Gradle project name as a
     * final fallback) is used. The value is sanitized the same way as for C-interop.
     *
     * @since 1.1.0
     */
    public val nameOverride: Property<String> = factory.property(String::class.java)

    /**
     * The root directory for JNI sources.
     *
     * Defaults to `<projectDir>/jni`. The actual native project will live under
     * `<projectDirectory>/<projectName>/src` — mirroring the C-interop layout.
     *
     * @since 1.1.0
     */
    public val projectDirectory: DirectoryProperty = factory.directoryProperty()

    /**
     * Additional C++ library include directories passed to the compiler.
     *
     * Each entry is added to the generated `CMakeLists.txt` via
     * `target_include_directories`, allowing the native project to resolve
     * headers from multiple external libraries located in different
     * directories. Paths may be absolute or relative to the native project
     * root (`<projectDirectory>/<projectName>`).
     *
     * Defaults to an empty list, in which case only the conventional `include`
     * directory and the JNI headers are used.
     *
     * @since 1.3.0
     */
    public val libraryIncludePaths: ListProperty<String> = factory.listProperty(String::class.java)

    /**
     * Additional library directories added to `java.library.path` at runtime.
     *
     * These paths are used by test and run tasks to resolve shared libraries
     * (`.so`, `.dylib`, `.dll`) at runtime. The paths are appended to the
     * directory the native build writes its own artifacts to.
     *
     * Paths can be absolute or relative to the Gradle project directory.
     *
     * @since 1.3.1
     */
    public val libraryRuntimePaths: ListProperty<String> = factory.listProperty(String::class.java)

    /**
     * The CMake build type used for both the configure and the build step.
     *
     * Defaults to `Release`. Set it to `Debug` to get unoptimized binaries with debug
     * symbols, which is what a native debugger attached to the JVM needs.
     *
     * @since 2.0.0
     */
    public val buildType: Property<String> = factory.property(String::class.java).convention("Release")

    /**
     * An explicit path to the CMake executable.
     *
     * When absent, CMake is looked up on the `PATH` and then in the conventional
     * installation directories. Set this when a build must pin an exact CMake
     * installation, for example on a locked down build agent.
     *
     * @since 2.0.0
     */
    public val cmakeExecutable: Property<String> = factory.property(String::class.java)

    /**
     * An explicit CMake generator, passed to `cmake -G`.
     *
     * When absent, CMake picks its platform default. Setting this matters mostly on
     * Windows, where the choice between the Visual Studio and Ninja generators decides
     * whether the build is single- or multi-configuration.
     *
     * @since 2.0.0
     */
    public val generator: Property<String> = factory.property(String::class.java)

    /**
     * Configuration for generating JNI headers from the compiled Kotlin and Java classes.
     *
     * @since 2.0.0
     */
    @get:Nested
    public abstract val headers: JniHeaderExtension

    /**
     * Configuration for packaging the built native libraries into the JAR.
     *
     * @since 2.0.0
     */
    @get:Nested
    public abstract val packaging: JniPackagingExtension

    /**
     * Configures JNI header generation.
     *
     * @param action The configuration action for [JniHeaderExtension].
     * @since 2.0.0
     */
    public fun headers(action: Action<JniHeaderExtension>) {
        action.execute(headers)
    }

    /**
     * Configures native library packaging.
     *
     * @param action The configuration action for [JniPackagingExtension].
     * @since 2.0.0
     */
    public fun packaging(action: Action<JniPackagingExtension>) {
        action.execute(packaging)
    }
}
