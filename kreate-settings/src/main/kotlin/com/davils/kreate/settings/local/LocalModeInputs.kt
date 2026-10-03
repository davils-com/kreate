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
import org.gradle.api.GradleException
import org.gradle.api.provider.ProviderFactory
import java.io.File

private const val TRUE_VALUE: String = "true"

private const val FALSE_VALUE: String = "false"

private const val LIBRARY_SEPARATOR: Char = ','

/**
 * The inputs the activation rule is evaluated against.
 *
 * Gathered into one value so that the settings plugin and the project plugin cannot drift into
 * answering the question differently — a build whose settings substitute but whose projects still
 * enforce lock files would be worse than either behaviour on its own.
 *
 * @since 3.2.0
 */
@InternalKreateApi
public data class LocalModeInputs(
    /**
     * What the state directory records.
     * @since 3.2.0
     */
    val workspace: LocalWorkspace,
    /**
     * The absolute path of the state directory, used in messages.
     * @since 3.2.0
     */
    val stateDirectory: String,
    /**
     * The value of [LOCAL_PROPERTY] or [LOCAL_VARIABLE], or `null` if unset.
     * @since 3.2.0
     */
    val requested: Boolean?,
    /**
     * The library names local mode is narrowed to, empty for all of them.
     * @since 3.2.0
     */
    val only: Set<String>,
    /**
     * Whether the build is running in CI.
     * @since 3.2.0
     */
    val continuousIntegration: Boolean,
    /**
     * The CI variable that was found, for the message.
     * @since 3.2.0
     */
    val ciVariable: String?
)

/**
 * Gathers the activation inputs from the build.
 *
 * Shared by both plugins, so that what the settings plugin resolves and whether the project plugin
 * enforces lock files can never disagree.
 *
 * @param providers The provider factory of the surrounding `Project` or `Settings`.
 * @param gradleUserHome The Gradle user home holding the state directory.
 * @param ciVariables The environment variables whose presence means CI.
 * @return The gathered inputs.
 * @since 3.2.0
 */
@InternalKreateApi
public fun gatherLocalModeInputs(
    providers: ProviderFactory,
    gradleUserHome: File,
    ciVariables: List<String>
): LocalModeInputs {
    val requestProperty = providers.gradleProperty(LOCAL_PROPERTY)
    val requested = requestProperty.orElse(providers.environmentVariable(LOCAL_VARIABLE)).orNull

    val ciVariable = ciVariables.firstOrNull { variable -> providers.isEnvironmentVariableSet(variable) }

    val directory = stateDirectoryOf(providers, gradleUserHome)

    return LocalModeInputs(
        workspace = localWorkspaceProvider(providers, directory).get(),
        stateDirectory = directory.absolutePath,
        requested = parseLocalRequest(requested),
        only = parseLocalOnly(providers.gradleProperty(LOCAL_ONLY_PROPERTY).orNull),
        continuousIntegration = ciVariable != null,
        ciVariable = ciVariable
    )
}

private fun ProviderFactory.isEnvironmentVariableSet(variable: String): Boolean {
    val value = environmentVariable(variable).getOrElse("")
    return value.isNotBlank()
}

/**
 * Parses the value of [LOCAL_PROPERTY].
 *
 * Only `true` and `false` are accepted, so that a typo is an error rather than a silent `false`.
 *
 * @param value The raw value, or `null` if unset.
 * @return `true`, `false`, or `null` when unset.
 * @throws GradleException If the value is neither `true` nor `false`.
 * @since 3.2.0
 */
@InternalKreateApi
public fun parseLocalRequest(value: String?): Boolean? {
    val trimmed = value.trimmedOrNull() ?: return null

    val isTrue = trimmed.equals(TRUE_VALUE, ignoreCase = true)
    if (isTrue) return true

    val isFalse = trimmed.equals(FALSE_VALUE, ignoreCase = true)
    if (isFalse) return false

    throw GradleException("'$LOCAL_PROPERTY' has to be 'true' or 'false', but was '$trimmed'.")
}

/**
 * Parses the value of [LOCAL_ONLY_PROPERTY].
 *
 * @param value The raw comma separated value, or `null` if unset.
 * @return The library names, empty when unset.
 * @since 3.2.0
 */
@InternalKreateApi
public fun parseLocalOnly(value: String?): Set<String> {
    val entries = value?.split(LIBRARY_SEPARATOR).orEmpty()
    val names = entries.mapNotNull { entry -> entry.trimmedOrNull() }
    return names.toSet()
}
