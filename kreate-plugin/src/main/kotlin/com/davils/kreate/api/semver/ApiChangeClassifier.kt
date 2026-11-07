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
package com.davils.kreate.api.semver

private const val FINAL: String = "final"

private const val ABSTRACT: String = "abstract"

private const val STATIC: String = "static"

private const val PUBLIC: String = "public"

private val NARROWING_MODIFIERS: Set<String> = setOf(FINAL, ABSTRACT)

internal object ApiChangeClassifier {
    fun classify(before: Map<String, DumpClass>, after: Map<String, DumpClass>): List<ApiChange> {
        val removed = (before.keys - after.keys).sorted().map { name -> breaking("$name was removed") }
        val added = (after.keys - before.keys).sorted().map { name -> additive("$name was added") }
        val kept = before.keys.intersect(after.keys).sorted()
        val changed = kept.flatMap { name -> changesOf(before.getValue(name), after.getValue(name)) }
        return removed + changed + added
    }

    private fun changesOf(old: DumpClass, new: DumpClass): List<ApiChange> =
        headerChangesOf(old, new) + memberChangesOf(old, new)

    private fun headerChangesOf(old: DumpClass, new: DumpClass): List<ApiChange> {
        val name = new.name
        val changes = mutableListOf<ApiChange>()
        if (old.isExtensible && !new.isExtensible) changes += breaking("$name became final")
        val becameAbstract = ABSTRACT !in old.modifiers && ABSTRACT in new.modifiers
        if (becameAbstract && !new.isInterface) changes += breaking("$name became abstract")
        if (old.isInterface != new.isInterface) changes += breaking("$name changed between class and interface")
        val lost = (old.supertypes - new.supertypes).sorted()
        val gained = (new.supertypes - old.supertypes).sorted()
        changes += lost.map { type -> breaking("$name no longer extends $type") }
        changes += gained.map { type -> additive("$name now extends $type") }
        return changes
    }

    private fun memberChangesOf(old: DumpClass, new: DumpClass): List<ApiChange> {
        val removed = (old.members.keys - new.members.keys).sorted().map { identity ->
            breaking("${new.name}.${describe(old.members.getValue(identity))} was removed")
        }
        val added = (new.members.keys - old.members.keys).sorted().map { identity ->
            addedMemberChange(old, new.members.getValue(identity))
        }
        val kept = old.members.keys.intersect(new.members.keys).sorted()
        val modified = kept.mapNotNull { identity ->
            modifierChange(old, old.members.getValue(identity), new.members.getValue(identity))
        }
        return removed + modified + added
    }

    private fun addedMemberChange(owner: DumpClass, member: DumpMember): ApiChange {
        val description = "${owner.name}.${describe(member)}"
        val mustBeImplemented = ABSTRACT in member.modifiers && owner.isExtensible
        if (mustBeImplemented) return breaking("$description was added, and every implementation must provide it")
        return additive("$description was added")
    }

    private fun modifierChange(owner: DumpClass, old: DumpMember, new: DumpMember): ApiChange? {
        val description = "${owner.name}.${describe(new)}"
        val gained = new.modifiers - old.modifiers
        val narrowing = gained.intersect(NARROWING_MODIFIERS)
        val narrowed = narrowing.filter { modifier -> owner.isExtensible || modifier != FINAL }
        if (narrowed.isNotEmpty()) return breaking("$description became ${narrowed.sorted().joinToString(" ")}")
        val wasStatic = STATIC in old.modifiers
        if (wasStatic != STATIC in new.modifiers) return breaking("$description changed between static and instance")
        if (PUBLIC in old.modifiers && PUBLIC !in new.modifiers) return breaking("$description is no longer public")
        return null
    }

    private fun describe(member: DumpMember): String = "${member.name} ${member.descriptor}"

    private fun breaking(description: String): ApiChange = ApiChange(ApiChangeKind.BREAKING, description)

    private fun additive(description: String): ApiChange = ApiChange(ApiChangeKind.ADDITIVE, description)
}
