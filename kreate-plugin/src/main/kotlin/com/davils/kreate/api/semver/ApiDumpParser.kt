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

private val CLASS_HEADER: Regex = Regex("""^(.*?)\b(class|interface) (\S+)(?: : (.+?))? \{$""")

private val MEMBER: Regex = Regex("""^(.*?)\b(fun|field) (\S+) (\S+)$""")

private const val CLASS_END: String = "}"

private const val INTERFACE: String = "interface"

private const val MODIFIERS: Int = 1

private const val KEYWORD: Int = 2

private const val NAME: Int = 3

private const val LAST: Int = 4

internal class ApiDumpParser private constructor() {
    private val classes: MutableList<DumpClass> = mutableListOf()

    private val members: MutableList<DumpMember> = mutableListOf()

    private var header: MatchResult? = null

    private fun read(line: String) {
        val open = header ?: return start(line)
        if (line == CLASS_END) return finish(open)
        MEMBER.matchEntire(line)?.let { member -> members += memberOf(member.groupValues) }
    }

    private fun start(line: String) {
        header = CLASS_HEADER.matchEntire(line)
        members.clear()
    }

    private fun finish(open: MatchResult) {
        classes += classOf(open.groupValues, members.toList())
        header = null
    }

    private fun classOf(values: List<String>, members: List<DumpMember>): DumpClass {
        val parents = values[LAST].split(',').map { type -> type.trim() }
        val supertypes = parents.filter { type -> type.isNotEmpty() }.toSet()
        val byIdentity = members.associateBy { member -> member.identity }
        val modifiers = wordsOf(values[MODIFIERS])
        val isInterface = values[KEYWORD] == INTERFACE || INTERFACE in modifiers
        return DumpClass(values[NAME], modifiers - INTERFACE, isInterface, supertypes, byIdentity)
    }

    private fun memberOf(values: List<String>): DumpMember =
        DumpMember(wordsOf(values[MODIFIERS]), values[KEYWORD], values[NAME], values[LAST])

    private fun wordsOf(text: String): Set<String> {
        val words = text.split(' ')
        return words.filter { word -> word.isNotBlank() }.toSet()
    }

    companion object {
        fun parse(dump: String): Map<String, DumpClass> {
            val parser = ApiDumpParser()
            dump.lines().forEach { line -> parser.read(line.trim()) }
            return parser.classes.associateBy { dumpClass -> dumpClass.name }
        }
    }
}
