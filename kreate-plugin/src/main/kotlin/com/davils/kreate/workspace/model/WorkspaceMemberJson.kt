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
package com.davils.kreate.workspace.model

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object WorkspaceMemberJson {
    fun render(member: WorkspaceMember): String {
        val modules = member.modules.toSortedMap().mapValues { (_, version) -> JsonPrimitive(version) }
        val projects = member.projects.sorted().map(::JsonPrimitive)
        return JsonObject(
            mapOf(
                "path" to JsonPrimitive(member.path),
                "directory" to JsonPrimitive(member.directory),
                "projects" to JsonArray(projects),
                "modules" to JsonObject(modules)
            )
        ).toString()
    }

    fun parse(text: String): WorkspaceMember? {
        val root = parsed(text) as? JsonObject ?: return null
        val path = textOf(root["path"]) ?: return null
        val projects = (root["projects"] as? JsonArray).orEmpty().mapNotNull(::textOf)
        val modules = (root["modules"] as? JsonObject).orEmpty().mapValues { (_, value) -> textOf(value).orEmpty() }
        return WorkspaceMember(path, textOf(root["directory"]).orEmpty(), projects.toSet(), modules)
    }

    private fun parsed(text: String): JsonElement? = try {
        Json.parseToJsonElement(text)
    } catch (_: SerializationException) {
        null
    }

    private fun textOf(element: JsonElement?): String? = (element as? JsonPrimitive)?.content
}
