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

import org.gradle.api.Project
import org.gradle.api.provider.Provider

private const val GITHUB_REFERENCE_TYPE: String = "GITHUB_REF_TYPE"

private const val GITHUB_REFERENCE_NAME: String = "GITHUB_REF_NAME"

private const val GITHUB_TAG: String = "tag"

private const val GITLAB_TAG: String = "CI_COMMIT_TAG"

internal fun Project.ciReleaseTag(): Provider<String> {
    val referenceType = providers.environmentVariable(GITHUB_REFERENCE_TYPE)
    val referenceName = providers.environmentVariable(GITHUB_REFERENCE_NAME)
    val isGitHubTag = referenceType.filter { type -> type == GITHUB_TAG }
    val gitHubTag = isGitHubTag.zip(referenceName) { _, name -> name }
    val gitLabTag = providers.environmentVariable(GITLAB_TAG).filter { tag -> tag.isNotBlank() }
    return gitHubTag.orElse(gitLabTag)
}
