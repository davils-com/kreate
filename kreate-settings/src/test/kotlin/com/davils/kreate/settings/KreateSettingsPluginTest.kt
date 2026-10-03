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

package com.davils.kreate.settings

import com.davils.kreate.settings.local.LocalLibrary
import com.davils.kreate.settings.local.LocalModule
import com.davils.kreate.settings.local.LocalWorkspace
import com.davils.kreate.settings.local.MAVEN_REPO_LOCAL_PROPERTY
import com.davils.kreate.settings.local.fakeProviders
import com.davils.kreate.settings.local.freshDirectory
import com.davils.kreate.settings.wiring.DeactivateLockingAction
import com.davils.kreate.settings.wiring.LocalResolutionAction
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.IsolatedAction
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.invocation.GradleLifecycle
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.ProviderFactory
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

class KreateSettingsPluginTest : FunSpec({

    val workspace = tempdir()

    val published = LocalWorkspace(
        listOf(
            LocalLibrary(
                library = "core",
                group = "com.example",
                repository = File("/workspace/core"),
                version = "1.0.0-SNAPSHOT",
                publishedAt = null,
                kreateVersion = "3.2.0",
                modules = listOf(LocalModule(group = "com.example", name = "core"))
            )
        )
    )

    fun extension(): KreateSettingsExtension {
        val builder = ProjectBuilder.builder()
        val project = builder.withProjectDir(workspace.freshDirectory("project")).build()
        return project.objects.newInstance(KreateSettingsExtension::class.java)
    }

    data class Build(
        val settings: Settings,
        val extensions: ExtensionContainer,
        val lifecycle: GradleLifecycle,
        val evaluated: Action<in Settings>
    )

    fun build(extension: KreateSettingsExtension, providers: ProviderFactory): Build {
        val extensions = mockk<ExtensionContainer>()
        every { extensions.create("kreateSettings", KreateSettingsExtension::class.java, *anyVararg()) } returns
            extension

        val lifecycle = mockk<GradleLifecycle>(relaxed = true)
        val evaluated = slot<Action<in Settings>>()
        val gradle = mockk<Gradle>()
        every { gradle.settingsEvaluated(capture(evaluated)) } returns Unit
        every { gradle.gradleUserHomeDir } returns workspace.freshDirectory("home")
        every { gradle.lifecycle } returns lifecycle

        val settings = mockk<Settings>()
        every { settings.extensions } returns extensions
        every { settings.gradle } returns gradle
        every { settings.providers } returns providers

        KreateSettingsPlugin().apply(settings)
        return Build(
            settings = settings,
            extensions = extensions,
            lifecycle = lifecycle,
            evaluated = evaluated.captured
        )
    }

    fun Build.evaluate() {
        evaluated.execute(settings)
    }

    fun publishedProviders(repository: File, environment: Map<String, String> = emptyMap()): ProviderFactory =
        fakeProviders(
            environment = environment,
            systemProperties = mapOf(MAVEN_REPO_LOCAL_PROPERTY to repository.path),
            workspace = published
        )

    fun beforeProjectOf(build: Build): IsolatedAction<in Project> {
        val captured = slot<IsolatedAction<in Project>>()
        verify { build.lifecycle.beforeProject(capture(captured)) }
        return captured.captured
    }

    context("applying the plugin") {

        test("registers the kreateSettings extension") {
            val created = build(extension(), fakeProviders())

            verify { created.extensions.create("kreateSettings", KreateSettingsExtension::class.java, *anyVararg()) }
        }

        test("defers everything else until the settings script was evaluated") {
            val created = build(extension(), publishedProviders(workspace.freshDirectory()))

            verify(exactly = 0) { created.lifecycle.beforeProject(any()) }
        }
    }

    context("once the settings were evaluated") {

        test("does nothing when the repository switched local mode off") {
            val disabled = extension()
            disabled.enabled.set(false)
            val created = build(disabled, publishedProviders(workspace.freshDirectory()))

            created.evaluate()

            verify(exactly = 0) { created.lifecycle.beforeProject(any()) }
            verify(exactly = 0) { created.lifecycle.afterProject(any()) }
        }

        test("does nothing when nothing is published") {
            val created = build(extension(), fakeProviders())

            created.evaluate()

            verify(exactly = 0) { created.lifecycle.beforeProject(any()) }
            verify(exactly = 0) { created.lifecycle.afterProject(any()) }
        }

        test("fails in CI when the runner carries local state") {
            val created = build(extension(), publishedProviders(workspace.freshDirectory(), mapOf("CI" to "true")))

            val failure = shouldThrow<GradleException> { created.evaluate() }

            failure.message.orEmpty() shouldContain "while running in CI"
        }

        test("installs the local resolution before every project") {
            val created = build(extension(), publishedProviders(workspace.freshDirectory()))

            created.evaluate()

            beforeProjectOf(created).shouldBeInstanceOf<LocalResolutionAction>()
        }

        test("switches dependency locking off after every project") {
            val created = build(extension(), publishedProviders(workspace.freshDirectory()))

            created.evaluate()

            val captured = slot<IsolatedAction<in Project>>()
            verify { created.lifecycle.afterProject(capture(captured)) }
            captured.captured.shouldBeInstanceOf<DeactivateLockingAction>()
        }

        test("points the injected repository at the local Maven repository under the configured name") {
            val repository = workspace.freshDirectory("m2")
            val configured = extension()
            configured.repositoryName.set("Sibling")
            val created = build(configured, publishedProviders(repository))
            created.evaluate()

            val repositories = mockk<RepositoryHandler>()
            val repositoryAction = slot<Action<in MavenArtifactRepository>>()
            every { repositories.maven(capture(repositoryAction)) } returns mockk()
            val project = mockk<Project>()
            every { project.repositories } returns repositories
            every { project.configurations } returns mockk(relaxed = true)
            beforeProjectOf(created).execute(project)

            val injected = mockk<MavenArtifactRepository>(relaxed = true)
            repositoryAction.captured.execute(injected)

            verify { injected.name = "Sibling" }
            verify { injected.setUrl(repository.absoluteFile.toURI().toString()) }
        }
    }
})
