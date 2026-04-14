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

package com.davils.kreate.module.platform

import com.davils.kreate.KreateExtension
import com.davils.kreate.module.Module
import com.davils.kreate.module.platform.jvm.initializeJvmCompiler
import com.davils.kreate.module.platform.multiplatform.cinterop.initializeCInterop
import com.davils.kreate.module.platform.multiplatform.initializeMultiplatformCompiler
import org.gradle.api.Project

internal object PlatformModule : Module {
    override fun apply(project: Project, extension: KreateExtension) {
        project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            project.configureCommon(extension)
            project.configureMultiplatform(extension)
        }

        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            project.configureCommon(extension)
            project.configureJvm(extension)
        }
    }

    private fun Project.configureCommon(extension: KreateExtension) = afterEvaluate {
        configureJava(extension)
    }

    private fun Project.configureJvm(extension: KreateExtension): Unit = afterEvaluate {
        initializeJvmCompiler(extension)
    }

    private fun Project.configureMultiplatform(extension: KreateExtension): Unit = afterEvaluate {
        initializeMultiplatformCompiler(extension)
        initializeCInterop(extension)
    }
}