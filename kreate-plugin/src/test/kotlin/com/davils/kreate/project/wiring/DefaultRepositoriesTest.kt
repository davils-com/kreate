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

package com.davils.kreate.project.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreatePlugin
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder

class DefaultRepositoriesTest : FunSpec({
    fun evaluated(applyDefaults: Boolean): Project {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(KreatePlugin::class.java)
        val kreate = project.extensions.getByType(KreateExtension::class.java)
        kreate.project.applyDefaultRepositories.set(applyDefaults)
        (project as ProjectInternal).evaluate()
        return project
    }

    fun repositoryHostsOf(project: Project): List<String> {
        val repositories = project.repositories.withType(MavenArtifactRepository::class.java)
        return repositories.map { repository -> repository.url.host }
    }

    context("default repositories") {
        test("adds no repository unless asked to") {
            repositoryHostsOf(evaluated(applyDefaults = false)).shouldBeEmpty()
        }

        test("adds Maven Central, the Gradle Plugin Portal and Google when asked to") {
            val hosts = repositoryHostsOf(evaluated(applyDefaults = true))

            hosts shouldContainExactly listOf("repo.maven.apache.org", "plugins.gradle.org", "dl.google.com")
        }
    }
})
