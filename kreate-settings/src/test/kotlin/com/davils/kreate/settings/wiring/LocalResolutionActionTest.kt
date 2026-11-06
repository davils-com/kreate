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
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.MavenRepositoryContentDescriptor
import org.gradle.api.artifacts.repositories.RepositoryContentDescriptor
import java.util.concurrent.TimeUnit

class LocalResolutionActionTest : FunSpec({

    val repositoryUri = "file:/home/dev/.m2/repository/"

    fun action(vararg substitutions: Pair<String, String>) = LocalResolutionAction(
        substitutions = hashMapOf(*substitutions),
        repositoryName = "KreateLocal",
        repositoryUri = repositoryUri
    )

    fun project(repositories: RepositoryHandler, configurations: ConfigurationContainer): Project {
        val project = mockk<Project>()
        every { project.repositories } returns repositories
        every { project.configurations } returns configurations
        return project
    }

    fun repositoryActionOf(action: LocalResolutionAction): Action<in MavenArtifactRepository> {
        val repositories = mockk<RepositoryHandler>()
        val captured = slot<Action<in MavenArtifactRepository>>()
        every { repositories.maven(capture(captured)) } returns mockk()
        action.execute(project(repositories, mockk(relaxed = true)))
        return captured.captured
    }

    fun configuredRepository(action: LocalResolutionAction): MavenArtifactRepository {
        val repository = mockk<MavenArtifactRepository>(relaxed = true)
        repositoryActionOf(action).execute(repository)
        return repository
    }

    fun contentActionOf(action: LocalResolutionAction): Action<in RepositoryContentDescriptor> {
        val repository = mockk<MavenArtifactRepository>(relaxed = true)
        val captured = slot<Action<in RepositoryContentDescriptor>>()
        every { repository.content(capture(captured)) } returns Unit
        repositoryActionOf(action).execute(repository)
        return captured.captured
    }

    fun includedContent(action: LocalResolutionAction): RepositoryContentDescriptor {
        val descriptor = mockk<RepositoryContentDescriptor>(relaxed = true)
        contentActionOf(action).execute(descriptor)
        return descriptor
    }

    fun strategyActionOf(action: LocalResolutionAction): Action<in ResolutionStrategy> {
        val configurations = mockk<ConfigurationContainer>()
        val configurationAction = slot<Action<in Configuration>>()
        every { configurations.configureEach(capture(configurationAction)) } returns Unit
        action.execute(project(mockk(relaxed = true), configurations))

        val configuration = mockk<Configuration>()
        val strategyAction = slot<Action<in ResolutionStrategy>>()
        every { configuration.resolutionStrategy(capture(strategyAction)) } returns configuration
        configurationAction.captured.execute(configuration)
        return strategyAction.captured
    }

    fun configuredStrategy(action: LocalResolutionAction): ResolutionStrategy {
        val strategy = mockk<ResolutionStrategy>(relaxed = true)
        strategyActionOf(action).execute(strategy)
        return strategy
    }

    context("the injected repository") {

        test("carries the configured name and location") {
            val repository = configuredRepository(action("com.example:core" to "1.0.0-SNAPSHOT"))

            verify { repository.name = "KreateLocal" }
            verify { repository.setUrl(repositoryUri) }
        }

        test("serves snapshots only") {
            val repository = mockk<MavenArtifactRepository>(relaxed = true)
            val captured = slot<Action<in MavenRepositoryContentDescriptor>>()
            every { repository.mavenContent(capture(captured)) } returns Unit
            repositoryActionOf(action("com.example:core" to "1.0.0-SNAPSHOT")).execute(repository)

            val descriptor = mockk<MavenRepositoryContentDescriptor>(relaxed = true)
            captured.captured.execute(descriptor)

            verify { descriptor.snapshotsOnly() }
        }

        test("serves exactly the published modules") {
            val descriptor = includedContent(
                action("com.example:core" to "1.0.0-SNAPSHOT", "com.example:net" to "2.0.0-SNAPSHOT")
            )

            verify { descriptor.includeModule("com.example", "core") }
            verify { descriptor.includeModule("com.example", "net") }
        }

        test("skips a coordinate whose group or name is empty") {
            val descriptor = includedContent(
                action(":orphan" to "1.0.0-SNAPSHOT", "com.example:" to "1.0.0-SNAPSHOT")
            )

            verify(exactly = 0) { descriptor.includeModule(any(), any()) }
        }
    }

    context("every configuration") {

        test("forces the published versions") {
            val strategy = configuredStrategy(action("com.example:core" to "1.0.0-SNAPSHOT"))

            verify { strategy.force("com.example:core:1.0.0-SNAPSHOT") }
        }

        test("never caches a changing local module") {
            val strategy = configuredStrategy(action("com.example:core" to "1.0.0-SNAPSHOT"))

            verify { strategy.cacheChangingModulesFor(0, TimeUnit.SECONDS) }
        }
    }
})
