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

import com.davils.kreate.platform.MultiplatformTargetsExtension
import com.davils.kreate.platform.WebEnvironment
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

internal fun Project.declareTargets(targets: MultiplatformTargetsExtension) {
    val kotlin = extensions.getByType<KotlinMultiplatformExtension>()

    if (targets.jvm.get()) kotlin.jvm()

    val jsEnvironments = targets.js.get()
    if (jsEnvironments.isNotEmpty()) kotlin.js { inEnvironments(jsEnvironments) }

    val wasmJsEnvironments = targets.wasmJs.get()
    if (wasmJsEnvironments.isNotEmpty()) kotlin.declareWasmJs(wasmJsEnvironments)
}

internal fun Project.rejectTargets(targets: MultiplatformTargetsExtension) {
    val declaresJvm = targets.jvm.get()
    val declaresWeb = targets.js.get().isNotEmpty() || targets.wasmJs.get().isNotEmpty()
    if (!declaresJvm && !declaresWeb) return

    throw GradleException(
        """
            Project '$path' applies the Kotlin JVM plugin and asks Kreate for multiplatform targets.

            Targets need the Kotlin Multiplatform plugin. Apply org.jetbrains.kotlin.multiplatform
            instead of org.jetbrains.kotlin.jvm, or remove
            `kreate { platform { multiplatform { targets { } } } }`.
        """.trimIndent()
    )
}

@OptIn(ExperimentalWasmDsl::class)
private fun KotlinMultiplatformExtension.declareWasmJs(environments: Set<WebEnvironment>) {
    wasmJs { inEnvironments(environments) }
}

private fun KotlinJsTargetDsl.inEnvironments(environments: Set<WebEnvironment>) {
    if (WebEnvironment.BROWSER in environments) browser()
    if (WebEnvironment.NODE in environments) nodejs()
}
