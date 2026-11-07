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
package com.davils.kreate.settings.wiring

import org.gradle.api.IsolatedAction
import org.gradle.api.Project

internal const val WORKSPACE_MEMBERS_PROPERTY: String = "kreate.workspace.members"

private const val ROOT_PATH: String = ":"

internal class WorkspaceMembersAction(private val members: String) : IsolatedAction<Project> {
    override fun execute(target: Project) {
        if (target.path != ROOT_PATH) return
        target.extensions.extraProperties.set(WORKSPACE_MEMBERS_PROPERTY, members)
    }
}
