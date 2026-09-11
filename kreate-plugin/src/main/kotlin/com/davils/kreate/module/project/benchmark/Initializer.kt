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

package com.davils.kreate.module.project.benchmark

import com.davils.kreate.KreateExtension
import org.gradle.api.Project

/**
 * The id of the kotlinx-benchmark plugin Kreate applies.
 *
 * @since 2.2.0
 */
internal const val BENCHMARK_PLUGIN_ID: String = "org.jetbrains.kotlinx.benchmark"

/**
 * Initializes benchmark support for the project.
 *
 * The kotlinx-benchmark plugin itself is applied earlier, by `applyFeaturePlugins`.
 *
 * @param extension The main Kreate extension.
 * @since 2.2.0
 */
internal fun Project.initializeBenchmark(extension: KreateExtension) {
    val benchmarkExtension = extension.project.benchmark
    if (!benchmarkExtension.enabled.get()) return

    configureBenchmarks(benchmarkExtension)
}
