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

package com.davils.kreate.feature

import com.davils.kreate.KreateExtension
import com.davils.kreate.api.wiring.ApiValidationFeature
import com.davils.kreate.benchmark.wiring.BenchmarkFeature
import com.davils.kreate.cinterop.wiring.CInteropFeature
import com.davils.kreate.configuration.wiring.ConfigurationSchemaFeature
import com.davils.kreate.constants.wiring.BuildConstantsFeature
import com.davils.kreate.coverage.wiring.CoverageFeature
import com.davils.kreate.detekt.wiring.DetektFeature
import com.davils.kreate.docs.wiring.DocsFeature
import com.davils.kreate.jni.wiring.JniFeature
import com.davils.kreate.local.wiring.LocalWorkflowFeature
import com.davils.kreate.locking.wiring.DependencyLockingFeature
import com.davils.kreate.platform.wiring.PlatformFeature
import com.davils.kreate.project.wiring.PluginsFeature
import com.davils.kreate.project.wiring.RepositoriesFeature
import com.davils.kreate.project.wiring.VersionFeature
import com.davils.kreate.publish.wiring.PublishFeature
import com.davils.kreate.testing.wiring.TestingFeature
import com.davils.kreate.trivy.wiring.TrivyFeature
import org.gradle.api.Project

internal object KreateFeatures {
    val ALL: List<KreateFeature> = listOf(
        PluginsFeature,
        RepositoriesFeature,
        DependencyLockingFeature,
        VersionFeature,
        BuildConstantsFeature,
        DocsFeature,
        TestingFeature,
        PublishFeature,
        DetektFeature,
        ApiValidationFeature,
        ConfigurationSchemaFeature,
        BenchmarkFeature,
        CoverageFeature,
        PlatformFeature,
        CInteropFeature,
        JniFeature,
        TrivyFeature,
        LocalWorkflowFeature
    )

    fun ordered(): List<KreateFeature> = ALL.sortedBy { feature -> feature.phase }

    fun applyAll(project: Project, extension: KreateExtension) {
        val features = ordered()
        features.forEach { feature -> feature.apply(project, extension) }
    }
}
