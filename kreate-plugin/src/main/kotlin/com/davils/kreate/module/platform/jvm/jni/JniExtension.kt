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

package com.davils.kreate.module.platform.jvm.jni

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

/**
 * Configures generation of JNI headers from the project's compiled classes.
 *
 * Kotlin has no equivalent of `javac -h`, so the signatures of `external` functions
 * normally have to be transcribed into C++ by hand. A single typo in a mangled name such
 * as `Java_com_example_Foo_bar` compiles cleanly and only fails at runtime with an
 * `UnsatisfiedLinkError`. Generating the header removes that entire class of defect.
 *
 * @param factory The object factory used for creating properties.
 * @since 2.0.0
 */
public abstract class JniHeaderExtension @Inject constructor(
    /**
     * The object factory instance used to create Gradle properties.
     * @since 2.0.0
     */
    factory: ObjectFactory
) {
    /**
     * Whether headers are generated for every `native` method found in the compiled
     * classes.
     *
     * Defaults to `true`. The generated header is placed on the CMake include path
     * automatically, so a native source file only has to `#include` it.
     *
     * @since 2.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * The file name of the generated header, without a directory component.
     *
     * Defaults to `<projectName>_jni.h`.
     *
     * @since 2.0.0
     */
    public val fileName: Property<String> = factory.property(String::class.java)
}

/**
 * Configures packaging of the built native libraries into the project's JAR.
 *
 * Without packaging, a consumer of the published artifact has to install the shared library
 * separately and set `-Djava.library.path`. With packaging enabled the library travels inside the
 * JAR under `<resourcePath>/<os>-<arch>/`, where a loader finds and extracts it on first use.
 *
 * `<os>-<arch>` is [com.davils.kreate.system.currentPlatformId], which is the spelling
 * `com.davils.arc.platform.Platform.identifier` uses - `linux-x86_64`, not `linux-x64`. That is not
 * a detail: it is the only thing that makes what this writes and what a loader reads the same
 * path.
 *
 * @param factory The object factory used for creating properties.
 * @since 2.0.0
 */
public abstract class JniPackagingExtension @Inject constructor(
    /**
     * The object factory instance used to create Gradle properties.
     * @since 2.0.0
     */
    factory: ObjectFactory
) {
    /**
     * Whether the built native libraries are packaged into the JAR.
     *
     * Defaults to `false`, so that the behaviour of an existing build does not change
     * when it is upgraded.
     *
     * @since 2.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The directory inside the JAR the native libraries are placed in.
     *
     * The operating system and architecture segment is appended automatically, so the default
     * results in `native/linux-x86_64/libexample.so`.
     *
     * **The default was `natives` until 3.0.0**, which no Davils loader has ever looked in:
     * `com.davils:sira-native` reads `/native` and so did `com.davils.arc.jni` before it. Packaging
     * and loading were a directory apart, and the only symptom was a library that could not be found
     * at runtime.
     *
     * @since 2.0.0
     */
    public val resourcePath: Property<String> = factory.property(String::class.java).convention("native")

    /**
     * Whether a `digests.properties` manifest is written beside the packaged libraries.
     *
     * Defaults to `true`. The manifest carries the SHA-256 of every library this build produced,
     * under the platform it was built for, and it is what a consumer pins so that nobody has to
     * hash a release artifact by hand.
     *
     * It is not a check that runs itself: a digest read out of the same artifact as the binary it
     * describes was written by whoever wrote the binary. See
     * [com.davils.kreate.module.platform.jvm.jni.tasks.GenerateDigestManifest].
     *
     * @since 3.0.0
     */
    public val digestManifest: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * Whether a `KreateNativeLoader` object is generated into the project's sources.
     *
     * The generated loader first tries `System.loadLibrary`, so a developer's local run with
     * `java.library.path` set keeps working, and only falls back to extracting the packaged library
     * from the classpath.
     *
     * **Defaults to `false` since 3.0.0.** It is the dependency-free fallback, not the recommended
     * loader, and having it on by default made the weakest of the available loaders the one most
     * builds got. What it does not do:
     *
     * - it checks no digest, so it cannot tell the binary you shipped from one somebody replaced;
     * - it extracts into `Files.createTempDirectory`, which on a shared machine is world-readable
     *   and writable by every other user;
     * - it marks the copy `deleteOnExit`, so every run extracts again;
     * - it reports the first thing that went wrong rather than every place it looked.
     *
     * `com.davils:sira-native` does all four, and is what a Davils program should take. Turn this on
     * where the artifact must load with nothing on the classpath but itself.
     *
     * @since 2.0.0
     */
    public val generateLoader: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * Configuration for publishing the native libraries as separate per-platform artifacts.
     *
     * @since 2.2.0
     */
    @get:Nested
    public abstract val publishing: JniPublishingExtension

    /**
     * Configures the [JniPublishingExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 2.2.0
     */
    public fun publishing(action: Action<JniPublishingExtension>) {
        action.execute(publishing)
    }
}
