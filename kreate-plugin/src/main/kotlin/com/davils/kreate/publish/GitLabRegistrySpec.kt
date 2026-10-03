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

package com.davils.kreate.publish

import org.gradle.api.Action
import org.gradle.api.artifacts.repositories.RepositoryContentDescriptor

/**
 * Describes a GitLab Package Registry to resolve dependencies from.
 *
 * The values are read as the `repositories { }` block is evaluated rather than lazily, because a
 * repository has to exist before anything can be resolved from it. Plain properties are therefore
 * the honest type here, not Gradle `Property` instances that suggest a deferred value.
 *
 * @since 2.2.0
 */
public class GitLabRegistrySpec {
    /**
     * The Maven endpoint of the registry.
     *
     * GitLab exposes three levels, and which one you want depends on how your packages are
     * spread out:
     *
     * - `https://<host>/api/v4/groups/<id>/-/packages/maven` — every project in a group
     * - `https://<host>/api/v4/projects/<id>/packages/maven` — a single project
     * - `https://<host>/api/v4/packages/maven` — the instance, for the current user
     *
     * Required.
     *
     * @since 2.2.0
     */
    public var url: String? = null

    /**
     * The name Gradle reports the repository under in errors and `--info` output.
     *
     * Defaults to `GitlabPackageRegistry`. Worth changing when a build resolves from more than
     * one GitLab instance, because the name is what tells two `401`s apart.
     *
     * @since 2.2.0
     */
    public var name: String = "GitlabPackageRegistry"

    /**
     * The environment variable holding the CI job token.
     *
     * Defaults to `CI_JOB_TOKEN`, which GitLab injects into every pipeline job. When it is set,
     * it takes precedence and is sent as the `Job-Token` header — a job token is scoped to the
     * running pipeline and expires with it, which is what makes it the right credential in CI.
     *
     * @since 2.2.0
     */
    public var jobTokenVariable: String = "CI_JOB_TOKEN"

    /**
     * The Gradle property holding a personal or group access token.
     *
     * Defaults to `gitlabToken`. This is the credential a developer keeps in
     * `~/.gradle/gradle.properties`, outside the repository — which is the point, and why it is
     * checked before the environment variable.
     *
     * @since 2.2.0
     */
    public var tokenProperty: String = "gitlabToken"

    /**
     * The environment variable holding a personal or group access token.
     *
     * Defaults to `GITLAB_TOKEN`. Used when [tokenProperty] is unset, for shells and containers
     * that carry credentials in the environment.
     *
     * @since 2.2.0
     */
    public var tokenVariable: String = "GITLAB_TOKEN"

    /**
     * The header name used when authenticating with a personal or group access token.
     *
     * Defaults to `Private-Token`. A deploy token needs `Deploy-Token` instead — the two are
     * different credentials and GitLab rejects one sent under the other's header.
     *
     * @since 2.2.0
     */
    public var tokenHeader: String = "Private-Token"

    internal val contentActions: MutableList<Action<RepositoryContentDescriptor>> = mutableListOf()

    /**
     * Restricts what this repository is consulted for.
     *
     * Worth setting in almost every case. Without a filter Gradle asks the registry about every
     * dependency the build has, which is slow over an authenticated remote, and tells the
     * registry's operator the name of every artifact you depend on — including those from other
     * repositories.
     *
     * ```kotlin
     * gitlabPackageRegistry(providers) {
     *     url = "https://gitlab.example.com/api/v4/groups/42/-/packages/maven"
     *     content { includeGroup("com.example") }
     * }
     * ```
     *
     * @param action The configuration action applied to the repository's content descriptor.
     * @since 2.2.0
     */
    public fun content(action: Action<RepositoryContentDescriptor>) {
        contentActions += action
    }
}
