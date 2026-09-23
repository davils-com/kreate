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

package com.davils.kreate.module.local

import com.davils.kreate.KreateTasks
import org.gradle.api.GradleException
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.ProviderFactory
import java.io.File
import java.io.Serializable

/**
 * The Gradle property that turns local mode off, or demands that it be on.
 *
 * @since 3.2.0
 */
internal const val LOCAL_PROPERTY: String = "davils.local"

/**
 * The environment variable equivalent of [LOCAL_PROPERTY].
 *
 * @since 3.2.0
 */
internal const val LOCAL_VARIABLE: String = "DAVILS_LOCAL"

/**
 * The Gradle property that narrows local mode to a subset of the published libraries.
 *
 * @since 3.2.0
 */
internal const val LOCAL_ONLY_PROPERTY: String = "davils.local.only"

/**
 * The Gradle property that requests a local publish without naming the task.
 *
 * @since 3.2.0
 */
internal const val LOCAL_PUBLISH_PROPERTY: String = "davils.local.publish"

/**
 * The environment variables whose presence means the build is running in CI.
 *
 * @since 3.2.0
 */
internal val DEFAULT_CI_VARIABLES: List<String> =
    listOf("CI", "GITLAB_CI", "GITHUB_ACTIONS", "CI_PIPELINE_ID")

/**
 * Whether Kreate resolves Davils artefacts from the local Maven repository for this build.
 *
 * @since 3.2.0
 */
internal sealed interface LocalMode : Serializable {
    /**
     * Local mode is on.
     *
     * @since 3.2.0
     */
    data class Active(
        /**
         * The libraries whose coordinates are substituted.
         * @since 3.2.0
         */
        val workspace: LocalWorkspace
    ) : LocalMode {
        private companion object {
            /**
             * The serial version identifier.
             * @since 3.2.0
             */
            private const val serialVersionUID: Long = 1L
        }
    }

    /**
     * Local mode is off.
     *
     * The reason is carried rather than discarded because `kreateLocalStatus` exists to answer
     * "why is my fix not being picked up", and "off" on its own never answers it.
     *
     * @since 3.2.0
     */
    data class Inactive(
        /**
         * A sentence naming what turned local mode off.
         * @since 3.2.0
         */
        val reason: String
    ) : LocalMode {
        private companion object {
            /**
             * The serial version identifier.
             * @since 3.2.0
             */
            private const val serialVersionUID: Long = 1L
        }
    }
}

/**
 * Whether this mode substitutes anything.
 *
 * @since 3.2.0
 */
internal val LocalMode.isActive: Boolean get() = this is LocalMode.Active

/**
 * The workspace being substituted, or an empty one when local mode is off.
 *
 * @since 3.2.0
 */
internal val LocalMode.workspace: LocalWorkspace
    get() = (this as? LocalMode.Active)?.workspace ?: LocalWorkspace.EMPTY

/**
 * The inputs the activation rule is evaluated against.
 *
 * Gathered into one value so that the settings plugin and the project plugin cannot drift into
 * answering the question differently — a build whose settings substitute but whose projects still
 * enforce lock files would be worse than either behaviour on its own.
 *
 * @since 3.2.0
 */
internal data class LocalModeInputs(
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
 * Decides whether local mode is on.
 *
 * The rule, in order:
 *
 * 1. Explicitly switched off — off, unconditionally. This is the escape hatch, so nothing below
 *    may override it.
 * 2. Running in CI — off. If the state directory is non-empty as well, the build *fails*: a
 *    runner that carries a developer's local state would resolve artefacts nobody else has, and
 *    silently producing a release from it is the one outcome this feature must never allow.
 * 3. Nothing published — off, because there is nothing to substitute. If local mode was demanded
 *    explicitly, fail instead of quietly doing nothing.
 * 4. Otherwise — on.
 *
 * Note what is *not* in the list: there is no step where a developer has to switch local mode on.
 * Publishing is what turns it on, which is the only sequence that cannot be got wrong by
 * forgetting a flag.
 *
 * @param inputs The gathered inputs.
 * @return Whether local mode is on, and if not, why.
 * @throws GradleException In the two cases above that must not be allowed to pass quietly.
 * @since 3.2.0
 */
internal fun resolveLocalMode(inputs: LocalModeInputs): LocalMode {
    // The escape hatch comes first and nothing below may override it — including the CI check,
    // which would otherwise turn an explicit opt-out into a build failure.
    if (inputs.requested == false) {
        return LocalMode.Inactive("it was switched off with -P$LOCAL_PROPERTY=false")
    }

    return resolveRequestedLocalMode(inputs)
}

/**
 * Decides whether local mode is on, for a build that did not switch it off.
 *
 * @param inputs The gathered inputs.
 * @return Whether local mode is on, and if not, why.
 * @throws GradleException If the state must not be allowed to pass quietly.
 * @since 3.2.0
 */
private fun resolveRequestedLocalMode(inputs: LocalModeInputs): LocalMode {
    failIfCiCarriesLocalState(inputs)
    if (inputs.continuousIntegration) {
        return LocalMode.Inactive("the build is running in CI (${inputs.ciVariable} is set)")
    }

    val available = if (inputs.only.isEmpty()) {
        inputs.workspace
    } else {
        inputs.workspace.restrictedTo(inputs.only)
    }
    failIfDemandedButUnavailable(inputs, available)

    return if (available.isEmpty) {
        LocalMode.Inactive("nothing is published to the local Maven repository")
    } else {
        LocalMode.Active(available)
    }
}

/**
 * Fails when a CI runner carries a developer's local state.
 *
 * Not a warning. A pipeline that resolved a locally published snapshot would build against an
 * artefact that exists on one machine, and could then publish the result to a shared registry —
 * a release nobody can reproduce, and the one outcome this feature must never permit.
 *
 * @param inputs The gathered inputs.
 * @throws GradleException If the state directory is non-empty in CI.
 * @since 3.2.0
 */
private fun failIfCiCarriesLocalState(inputs: LocalModeInputs) {
    if (!inputs.continuousIntegration || inputs.workspace.isEmpty) return

    throw GradleException(
        """
            Kreate found local development state while running in CI.

                State directory: ${inputs.stateDirectory}
                Detected by:     ${inputs.ciVariable}
                Libraries:       ${inputs.workspace.libraries.joinToString { it.library }}

            A CI build must never resolve from the local Maven repository: the artefacts there
            exist on one machine only, so anything built against them cannot be reproduced and
            must not be released.

            Remove the directory from the runner image, or unset ${inputs.ciVariable} if this is
            not in fact a CI machine.
        """.trimIndent()
    )
}

/**
 * Fails when local mode was demanded explicitly but has nothing to substitute.
 *
 * Doing nothing quietly here would be worse than failing: the developer asked for their local
 * build to be used, and a green build that silently used the released version instead is exactly
 * the outcome they were trying to avoid.
 *
 * @param inputs The gathered inputs.
 * @param available The workspace after narrowing.
 * @throws GradleException If local mode was demanded and nothing is available.
 * @since 3.2.0
 */
private fun failIfDemandedButUnavailable(inputs: LocalModeInputs, available: LocalWorkspace) {
    if (inputs.requested != true || !available.isEmpty) return

    throw GradleException(
        """
            Local mode was requested with -P$LOCAL_PROPERTY=true, but nothing is published to the
            local Maven repository${narrowedBy(inputs.only)}.

                State directory: ${inputs.stateDirectory}

            Publish a library first:

                cd <library> && ./gradlew ${KreateTasks.Local.PUBLISH}

            or, for a whole workspace at once:

                ./gradlew ${KreateTasks.Local.PUBLISH_ALL}
        """.trimIndent()
    )
}

/**
 * Renders the narrowing clause of the "nothing published" message.
 *
 * @param only The library names local mode was narrowed to.
 * @return A clause naming the filter, or an empty string when there is none.
 * @since 3.2.0
 */
private fun narrowedBy(only: Set<String>): String =
    if (only.isEmpty()) "" else " under -P$LOCAL_ONLY_PROPERTY=${only.sorted().joinToString(",")}"

/**
 * Parses the value of [LOCAL_PROPERTY].
 *
 * Only `true` and `false` are accepted. A typo in a flag that switches dependency resolution has
 * to be an error rather than a silent `false`, which would look exactly like the feature not
 * working.
 *
 * @param value The raw value, or `null` if unset.
 * @return `true`, `false`, or `null` when unset.
 * @throws GradleException If the value is neither `true` nor `false`.
 * @since 3.2.0
 */
internal fun parseLocalRequest(value: String?): Boolean? {
    val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null

    return when {
        trimmed.equals("true", ignoreCase = true) -> true
        trimmed.equals("false", ignoreCase = true) -> false
        else -> throw GradleException(
            "'$LOCAL_PROPERTY' has to be 'true' or 'false', but was '$trimmed'."
        )
    }
}

/**
 * Parses the value of [LOCAL_ONLY_PROPERTY].
 *
 * @param value The raw comma separated value, or `null` if unset.
 * @return The library names, empty when unset.
 * @since 3.2.0
 */
internal fun parseLocalOnly(value: String?): Set<String> =
    value?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.toSet()
        .orEmpty()

/**
 * Gathers the activation inputs from the build.
 *
 * Shared by both plugins on purpose. The settings plugin decides what a build *resolves* and the
 * project plugin decides whether lock files are enforced; those two answers disagreeing would
 * produce a build that substitutes a snapshot and then fails lock verification against it, which
 * is a worse outcome than either half being switched off.
 *
 * @param providers The provider factory of the surrounding `Project` or `Settings`.
 * @param gradleUserHome The Gradle user home holding the state directory.
 * @param ciVariables The environment variables whose presence means CI.
 * @return The gathered inputs.
 * @since 3.2.0
 */
internal fun gatherLocalModeInputs(
    providers: ProviderFactory,
    gradleUserHome: File,
    ciVariables: List<String>
): LocalModeInputs {
    val requested = providers.gradleProperty(LOCAL_PROPERTY)
        .orElse(providers.environmentVariable(LOCAL_VARIABLE))
        .orNull

    val ciVariable = ciVariables.firstOrNull { variable ->
        providers.environmentVariable(variable).getOrElse("").isNotBlank()
    }

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

/**
 * Whether this invocation asked for a local publish.
 *
 * Two forms are accepted. The property is the explicit one, and is what `kreateLocalPublishAll`
 * passes to the subprocess it starts. The task name is the convenient one: typing
 * `./gradlew kreateLocalPublish` should not also require a flag that says what the task name
 * already says.
 *
 * The task names are read from the root build of the composite rather than from this build.
 * Gradle derives an included build's [Gradle.getStartParameter] from its parent's and clears the
 * requested task names, so `:some-library:kreateLocalPublish` is only visible from the root.
 *
 * @param gradle The build invocation.
 * @param providers The provider factory of the surrounding project.
 * @return `true` when the version should carry the snapshot suffix.
 * @since 3.2.0
 */
internal fun requestsLocalPublish(gradle: Gradle, providers: ProviderFactory): Boolean {
    val requestedByProperty = providers.gradleProperty(LOCAL_PUBLISH_PROPERTY)
        .map { it.equals("true", ignoreCase = true) }
        .getOrElse(false)
    if (requestedByProperty) return true

    var root: Gradle = gradle
    while (root.parent != null) root = requireNotNull(root.parent)

    return root.startParameter.taskNames.any { requested ->
        requested.substringAfterLast(':') == KreateTasks.Local.PUBLISH
    }
}
