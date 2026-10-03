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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeSortedBy
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class KreateFeaturesTest : FunSpec({
    context("KreateFeatures.ordered") {
        test("applies the phases in ascending order") {
            KreateFeatures.ordered().shouldBeSortedBy { feature -> feature.phase }
        }

        test("applies every registered feature exactly once") {
            KreateFeatures.ordered() shouldContainExactlyInAnyOrder KreateFeatures.ALL
        }

        test("keeps the declared order within the configuration phase") {
            val configuration = KreateFeatures.ordered().filter { feature ->
                feature.phase == FeaturePhase.CONFIGURATION
            }

            configuration shouldBe listOf(
                BuildConstantsFeature,
                DocsFeature,
                TestingFeature,
                PublishFeature,
                DetektFeature,
                ApiValidationFeature,
                ConfigurationSchemaFeature,
                BenchmarkFeature,
                CoverageFeature
            )
        }

        test("pins the complete application order") {
            KreateFeatures.ordered() shouldBe listOf(
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
        }
    }
})
