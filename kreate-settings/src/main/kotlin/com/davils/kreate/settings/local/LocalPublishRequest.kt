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

package com.davils.kreate.settings.local

import com.davils.kreate.settings.InternalKreateApi
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.ProviderFactory

private const val TASK_PATH_SEPARATOR: Char = ':'

/**
 * Whether this invocation asked for a local publish.
 *
 * Two forms are accepted: the [LOCAL_PUBLISH_PROPERTY] property, which `kreateLocalPublishAll`
 * passes to the subprocess it starts, and the [LocalTaskNames.PUBLISH] task name on the command
 * line.
 *
 * The task names are read from the root build of the composite, because Gradle clears the
 * requested task names in the [Gradle.getStartParameter] of an included build.
 *
 * @param gradle The build invocation.
 * @param providers The provider factory of the surrounding project.
 * @return `true` when the version should carry the snapshot suffix.
 * @since 3.2.0
 */
@InternalKreateApi
public fun requestsLocalPublish(gradle: Gradle, providers: ProviderFactory): Boolean {
    val publishProperty = providers.gradleProperty(LOCAL_PUBLISH_PROPERTY)
    val isRequestedByProperty = publishProperty.map { it.equals("true", ignoreCase = true) }.getOrElse(false)
    if (isRequestedByProperty) return true

    return gradle.rootBuild().startParameter.taskNames.any { requested ->
        requested.substringAfterLast(TASK_PATH_SEPARATOR) == LocalTaskNames.PUBLISH
    }
}

private fun Gradle.rootBuild(): Gradle = generateSequence(this) { build -> build.parent }.last()
