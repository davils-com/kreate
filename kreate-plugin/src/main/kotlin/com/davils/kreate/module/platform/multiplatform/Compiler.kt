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

import com.davils.kreate.KreateExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Initializes the Kotlin Multiplatform compiler options.
 *
 * This function configures the explicit API mode and warning handling
 * based on the [PlatformExtension].
 *
 * @param extension The Kreate configuration extension.
 * @since 1.0.0
 */
internal fun Project.initializeMultiplatformCompiler(extension: KreateExtension) {
    val platformConfig = extension.platform

    configure<KotlinMultiplatformExtension> {
        if (platformConfig.explicitApi.get()) {
            explicitApi()
        }

        compilerOptions {
            allWarningsAsErrors.set(platformConfig.allWarningsAsErrors.get())
        }
    }
}
