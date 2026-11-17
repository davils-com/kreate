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

package com.davils.kreate.floor.wiring

import com.davils.kreate.floor.version.VersionFloorRule
import org.gradle.api.Action
import org.gradle.api.artifacts.DependencyResolveDetails

internal class VersionFloorAction(private val rules: List<VersionFloorRule>) : Action<DependencyResolveDetails> {
    override fun execute(details: DependencyResolveDetails) {
        val requested = details.requested
        val rule = rules.firstOrNull { candidate ->
            candidate.raises(requested.group, requested.name, requested.version)
        } ?: return
        details.useVersion(rule.floor)
        details.because("Raised to ${rule.floor}, the version floor of the tool classpaths")
    }
}
