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

import com.davils.kreate.host.platformTaskSuffix

/**
 * The names of the tasks the JNI feature registers, for `tasks.named(...)` in a consumer build.
 *
 * @since 4.0.0
 */
public object JniTaskNames {
    /**
     * Scaffolds the native CMake project.
     *
     * @since 4.0.0
     */
    public const val INITIALIZE: String = "kreateJniInitialize"

    /**
     * Generates the JNI headers for the native methods.
     *
     * @since 4.0.0
     */
    public const val HEADERS: String = "kreateJniHeaders"

    /**
     * Runs the CMake configure step.
     *
     * @since 4.0.0
     */
    public const val CONFIGURE: String = "kreateJniConfigure"

    /**
     * Builds the native library with CMake.
     *
     * @since 4.0.0
     */
    public const val BUILD: String = "kreateJniBuild"

    /**
     * Generates the Kotlin loader for the native library.
     *
     * @since 4.0.0
     */
    public const val LOADER: String = "kreateJniLoader"

    /**
     * Writes the digest manifest of the packaged native libraries.
     *
     * @since 4.0.0
     */
    public const val DIGEST_MANIFEST: String = "kreateJniDigestManifest"

    /**
     * Builds every per-platform native JAR.
     *
     * @since 4.0.0
     */
    public const val NATIVE_JARS: String = "kreateJniNativeJars"

    /**
     * Verifies that every selected platform delivered a native library.
     *
     * @since 4.0.0
     */
    public const val VERIFY_PLATFORMS: String = "kreateJniVerifyPlatforms"

    private const val NATIVE_JAR_PREFIX: String = "kreateJniNativeJar"

    /**
     * The name of the task that packages the native library of one platform.
     *
     * @param platformId The platform identifier, such as `linux-x86_64`.
     * @return The task name for that platform.
     * @since 4.0.0
     */
    public fun nativeJar(platformId: String): String {
        val suffix = platformTaskSuffix(platformId)
        return NATIVE_JAR_PREFIX + suffix
    }
}
