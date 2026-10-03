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

package com.davils.kreate.api.abi

import org.objectweb.asm.Opcodes

internal object AbiRenderer {
    private const val OBJECT_INTERNAL_NAME = "java/lang/Object"
    private const val INTERFACE_KEYWORD = "interface"
    private const val CLASS_KEYWORD = "class"
    private const val SUPERTYPE_SEPARATOR = ", "
    private const val MODIFIER_SEPARATOR = " "
    private const val MEMBER_INDENT = '\t'

    fun render(classes: List<AbiClass>): String {
        if (classes.isEmpty()) return ""

        val sorted = classes.sortedBy { abiClass -> abiClass.internalName }
        return sorted.joinToString(separator = "") { abiClass -> renderClass(abiClass) }
    }

    private fun renderClass(abiClass: AbiClass): String = buildString {
        append(modifiers(abiClass.access))
        append(' ')
        append(typeKeyword(abiClass.access))
        append(' ')
        append(abiClass.internalName)
        appendSupertypes(supertypesOf(abiClass))
        appendLine(" {")
        sortMembers(abiClass.members).forEach { member ->
            append(MEMBER_INDENT)
            appendLine(renderMember(member))
        }
        appendBlockEnd()
    }

    private fun typeKeyword(access: Int): String {
        val isInterface = access and Opcodes.ACC_INTERFACE != 0
        if (isInterface) return INTERFACE_KEYWORD
        return CLASS_KEYWORD
    }

    private fun supertypesOf(abiClass: AbiClass): List<String> = buildList {
        abiClass.superName?.takeIf { superName -> superName != OBJECT_INTERNAL_NAME }?.let(::add)
        addAll(abiClass.interfaces)
    }

    private fun StringBuilder.appendSupertypes(supertypes: List<String>) {
        if (supertypes.isEmpty()) return

        append(" : ")
        append(supertypes.joinToString(SUPERTYPE_SEPARATOR))
    }

    private fun StringBuilder.appendBlockEnd() {
        appendLine("}")
        appendLine()
    }

    private fun sortMembers(members: List<AbiMember>): List<AbiMember> =
        members.sortedWith(
            compareBy({ it.kind != AbiMemberKind.FIELD }, { it.name }, { it.descriptor })
        )

    private fun renderMember(member: AbiMember): String =
        "${modifiers(member.access)} ${member.kind.keyword} ${member.name} ${member.descriptor}"

    private fun modifiers(access: Int): String = buildList {
        add(visibilityOf(access))
        if (access and Opcodes.ACC_STATIC != 0) add("static")
        if (access and Opcodes.ACC_FINAL != 0) add("final")
        if (access and Opcodes.ACC_ABSTRACT != 0) add("abstract")
        if (access and Opcodes.ACC_SYNTHETIC != 0) add("synthetic")
    }.joinToString(MODIFIER_SEPARATOR)

    private fun visibilityOf(access: Int): String {
        if (access and Opcodes.ACC_PUBLIC != 0) return "public"
        if (access and Opcodes.ACC_PROTECTED != 0) return "protected"
        if (access and Opcodes.ACC_PRIVATE != 0) return "private"
        return "packageprivate"
    }
}
