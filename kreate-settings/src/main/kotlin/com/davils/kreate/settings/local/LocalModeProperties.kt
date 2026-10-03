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

/**
 * The Gradle property that turns local mode off, or demands that it be on.
 *
 * @since 3.2.0
 */
@InternalKreateApi
public const val LOCAL_PROPERTY: String = "kreate.local"

/**
 * The environment variable equivalent of [LOCAL_PROPERTY].
 *
 * @since 3.2.0
 */
@InternalKreateApi
public const val LOCAL_VARIABLE: String = "KREATE_LOCAL"

/**
 * The Gradle property that narrows local mode to a subset of the published libraries.
 *
 * @since 3.2.0
 */
@InternalKreateApi
public const val LOCAL_ONLY_PROPERTY: String = "kreate.local.only"

/**
 * The Gradle property that requests a local publish without naming the task.
 *
 * @since 3.2.0
 */
@InternalKreateApi
public const val LOCAL_PUBLISH_PROPERTY: String = "kreate.local.publish"

/**
 * The environment variables whose presence means the build is running in CI.
 *
 * @since 3.2.0
 */
@InternalKreateApi
public val DEFAULT_CI_VARIABLES: List<String> =
    listOf("CI", "GITLAB_CI", "GITHUB_ACTIONS", "CI_PIPELINE_ID")
