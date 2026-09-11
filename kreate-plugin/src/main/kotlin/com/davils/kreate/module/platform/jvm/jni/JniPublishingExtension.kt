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

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Publishes the native libraries as one artifact per platform instead of bundling them into the
 * main JAR.
 *
 * A JNI library built on one machine can only contain that machine's binary, so a single
 * published artifact works on a single platform. The usual answer is a fat JAR assembled from a
 * build matrix over every supported operating system — which requires runners for every one of
 * them. Where that is not available, this is the alternative: each platform becomes its own
 * artifact, `com.example:mylib-linux-x86_64`, and a release publishes whichever ones it has.
 *
 * The generated loader needs no change for this. It resolves `natives/<os>-<arch>/` from the
 * classpath and does not care which JAR on that classpath provides it.
 *
 * Enabling this changes what the main JAR contains: the native libraries move out of it entirely,
 * so that no consumer silently receives the platform the library happened to be built on. Each
 * consumer declares the platform artifact it needs.
 *
 * @param factory The object factory used for creating properties.
 * @since 2.2.0
 */
public abstract class JniPublishingExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 2.2.0
     */
    factory: ObjectFactory
) {
    /**
     * Whether the native libraries are published as separate per-platform artifacts.
     *
     * Defaults to `false`, which leaves the packaging behaviour of an existing build unchanged:
     * the binary of the build host stays inside the main JAR.
     *
     * @since 2.2.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The platforms this release publishes, for example `listOf("linux-x86_64")`.
     *
     * This is a selection, not a promise about every version to come. Publishing a subset is a
     * supported state and not an error — a project whose infrastructure can only build Linux
     * publishes Linux, and adds platforms later without any consumer having to change anything.
     *
     * What *is* an error is selecting a platform whose binary is nowhere to be found: that gap is
     * always an accident, and `kreateJniVerifyPlatforms` fails on it rather than publishing a
     * release with a hole in it.
     *
     * Defaults to the platform the build is running on. Only the identifiers Kreate itself
     * produces are accepted — see the failure message for the list.
     *
     * @since 2.2.0
     */
    public val platforms: ListProperty<String> = factory.listProperty(String::class.java)

    /**
     * A directory holding native libraries built elsewhere, laid out as `<os>-<arch>/libfoo.so`.
     *
     * This is how a platform the current machine cannot build gets into a release: another
     * runner, a contributor, or a mirrored pipeline drops its binary here and the platform joins
     * the selection.
     *
     * A staged binary wins over one this build produced for the same platform, so a pipeline can
     * publish a binary it built under controlled conditions rather than whatever the publishing
     * runner happened to compile.
     *
     * Unset by default.
     *
     * @since 2.2.0
     */
    public val stagingDirectory: DirectoryProperty = factory.directoryProperty()
}
