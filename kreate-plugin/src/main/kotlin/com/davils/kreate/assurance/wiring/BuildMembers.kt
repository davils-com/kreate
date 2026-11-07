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
package com.davils.kreate.assurance.wiring

import org.gradle.api.Project
import org.gradle.api.provider.Provider

private const val MEMBERS_PROPERTY: String = "kreate.workspace.members"

internal fun Project.buildMembers(): Provider<Set<String>> {
    val extras = extensions.extraProperties
    val own = path
    return provider {
        val shared = extras.takeIf { values -> values.has(MEMBERS_PROPERTY) }
        val value = shared?.get(MEMBERS_PROPERTY)
        val text = value?.toString().orEmpty()
        val paths = text.split(',').map { member -> member.trim() }
        val members = paths.filter { member -> member.isNotEmpty() }.toSet()
        members.ifEmpty { setOf(own) }
    }
}
