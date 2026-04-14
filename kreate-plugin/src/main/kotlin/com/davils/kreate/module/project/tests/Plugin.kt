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

package com.davils.kreate.module.project.tests

import org.gradle.api.Project
import org.gradle.kotlin.dsl.hasPlugin
import io.kotest.framework.gradle.KotestPlugin
import com.google.devtools.ksp.gradle.KspGradleSubplugin

internal fun Project.validateKotestPlugin() {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        if (!plugins.hasPlugin(KspGradleSubplugin::class)) {
            throw IllegalStateException("You need to apply the 'ksp' plugin by yourself in multiplatform projects if you want to use kotest.")
        }

        if (!plugins.hasPlugin(KotestPlugin::class)) {
            throw IllegalStateException("You need to apply the 'kotest' plugin by yourself in multiplatform projects if you want to use kotest.")
        }
    }
}