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

package com.davils.kreate.settings.wiring

import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ResolutionStrategy

class DeactivateLockingActionTest : FunSpec({

    context("DeactivateLockingAction") {

        test("switches dependency locking off for every configuration") {
            val configurations = mockk<ConfigurationContainer>()
            val captured = slot<Action<in Configuration>>()
            every { configurations.configureEach(capture(captured)) } returns Unit
            val project = mockk<Project>()
            every { project.configurations } returns configurations

            DeactivateLockingAction().execute(project)

            val strategy = mockk<ResolutionStrategy>(relaxed = true)
            val configuration = mockk<Configuration>()
            every { configuration.resolutionStrategy } returns strategy
            captured.captured.execute(configuration)

            verify { strategy.deactivateDependencyLocking() }
        }
    }
})
