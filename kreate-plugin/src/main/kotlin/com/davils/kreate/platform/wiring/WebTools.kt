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

package com.davils.kreate.platform.wiring

import com.davils.kreate.platform.MultiplatformExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.EnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec

internal fun Project.useSettingsToolRepositories(multiplatform: MultiplatformExtension) {
    if (!multiplatform.toolsFromSettingsRepositories.getOrElse(false)) return

    withoutDownloadRepository(NodeJsPlugin::class.java, NodeJsEnvSpec::class.java)
    withoutDownloadRepository(WasmNodeJsPlugin::class.java, WasmNodeJsEnvSpec::class.java)
    withoutDownloadRepository(YarnPlugin::class.java, YarnRootEnvSpec::class.java)
    withoutDownloadRepository(WasmYarnPlugin::class.java, WasmYarnRootEnvSpec::class.java)
}

private fun <S : EnvSpec<*>> Project.withoutDownloadRepository(
    pluginType: Class<out Plugin<Project>>,
    specType: Class<S>
) {
    plugins.withType(pluginType).configureEach {
        val spec = extensions.getByType(specType)
        spec.downloadBaseUrl.set(null as String?)
    }
}
