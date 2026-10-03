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
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Configures packaging of the built native libraries into the project's JAR.
 *
 * Without packaging, a consumer of the published artifact has to install the shared library
 * separately and set `-Djava.library.path`. With packaging enabled the library travels inside the
 * JAR under `<resourcePath>/<os>-<arch>/`, where a loader finds and extracts it on first use.
 *
 * `<os>-<arch>` is the platform identifier of the build host, spelled the way
 * `com.davils.arc.platform.Platform.identifier` spells it - `linux-x86_64`, not `linux-x64`. That is
 * not a detail: it is the only thing that makes what this writes and what a loader reads the same
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
     * results in `native/linux-x86_64/libexample.so`. The default `native` is the directory
     * `com.davils:sira-native` reads from.
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
     * describes was written by whoever wrote the binary.
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
     * Defaults to `false`. It is the dependency-free fallback, not the recommended loader, and the
     * weakest of the available loaders should not be the one a build gets by default. What it does
     * not do:
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
