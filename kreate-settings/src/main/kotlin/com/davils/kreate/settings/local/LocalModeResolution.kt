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

/**
 * Decides whether local mode is on.
 *
 * The rule, in order:
 *
 * 1. Explicitly switched off — off, unconditionally. This is the escape hatch, so nothing below
 *    may override it, including the CI check.
 * 2. Running in CI — off. If the state directory is non-empty as well, the build *fails*: a
 *    runner that carries a developer's local state would resolve artefacts nobody else has.
 * 3. Nothing published — off, because there is nothing to substitute. If local mode was demanded
 *    explicitly, fail instead of quietly doing nothing.
 * 4. Otherwise — on.
 *
 * There is no step where a developer has to switch local mode on: publishing is what turns it on.
 *
 * @param inputs The gathered inputs.
 * @return Whether local mode is on, and if not, why.
 * @throws GradleException In the two cases above that must not be allowed to pass quietly.
 * @since 3.2.0
 */
@InternalKreateApi
public fun resolveLocalMode(inputs: LocalModeInputs): LocalMode {
    val isSwitchedOff = inputs.requested == false
    if (isSwitchedOff) return LocalMode.Inactive("it was switched off with -P$LOCAL_PROPERTY=false")

    return resolveRequestedLocalMode(inputs)
}

private fun resolveRequestedLocalMode(inputs: LocalModeInputs): LocalMode {
    failIfCiCarriesLocalState(inputs)
    if (inputs.continuousIntegration) {
        return LocalMode.Inactive("the build is running in CI (${inputs.ciVariable} is set)")
    }

    val available = availableWorkspace(inputs)
    failIfDemandedButUnavailable(inputs, available)
    if (available.isEmpty) return LocalMode.Inactive("nothing is published to the local Maven repository")

    return LocalMode.Active(available)
}

private fun availableWorkspace(inputs: LocalModeInputs): LocalWorkspace {
    if (inputs.only.isEmpty()) return inputs.workspace

    return inputs.workspace.restrictedTo(inputs.only)
}

private fun failIfCiCarriesLocalState(inputs: LocalModeInputs) {
    val carriesLocalState = inputs.continuousIntegration && !inputs.workspace.isEmpty
    if (!carriesLocalState) return

    val libraryNames = inputs.workspace.libraries.joinToString { it.library }
    throw GradleException(
        """
            Kreate found local development state while running in CI.

                State directory: ${inputs.stateDirectory}
                Detected by:     ${inputs.ciVariable}
                Libraries:       $libraryNames

            A CI build must never resolve from the local Maven repository: the artefacts there
            exist on one machine only, so anything built against them cannot be reproduced and
            must not be released.

            Remove the directory from the runner image, or unset ${inputs.ciVariable} if this is
            not in fact a CI machine.
        """.trimIndent()
    )
}

private fun failIfDemandedButUnavailable(inputs: LocalModeInputs, available: LocalWorkspace) {
    val isDemandedButUnavailable = inputs.requested == true && available.isEmpty
    if (!isDemandedButUnavailable) return

    throw GradleException(
        """
            Local mode was requested with -P$LOCAL_PROPERTY=true, but nothing is published to the
            local Maven repository${narrowedBy(inputs.only)}.

                State directory: ${inputs.stateDirectory}

            Publish a library first:

                cd <library> && ./gradlew ${LocalTaskNames.PUBLISH}

            or, for a whole workspace at once:

                ./gradlew ${LocalTaskNames.PUBLISH_ALL}
        """.trimIndent()
    )
}

private fun narrowedBy(only: Set<String>): String {
    if (only.isEmpty()) return ""

    val names = only.sorted().joinToString(",")
    return " under -P$LOCAL_ONLY_PROPERTY=$names"
}
