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
package com.davils.kreate.assurance.task

import com.davils.kore.core.text.SafeText
import com.davils.kore.core.text.closeMatches
import com.davils.kreate.assurance.rule.RuleCatalog
import com.davils.kreate.assurance.rule.RuleInfo
import com.davils.kreate.standard.StandardsTaskNames
import com.davils.kreate.standard.model.StandardId
import com.davils.kreate.standard.model.StandardIdGrammar
import com.davils.kreate.standard.model.StandardLinks
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.InvalidUserDataException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.api.tasks.options.Option

private val ADVISORY_ID: Regex = Regex("^(GHSA|CVE|OSV|PYSEC|GO|RUSTSEC)-[A-Za-z0-9-]+$")

private const val ADVISORY_URL: String = "https://osv.dev/vulnerability/"

@UntrackedTask(because = "It only prints the catalog")
internal abstract class AssuranceExplain : KreateTask(
    "Explains a rule of the catalog, or lists all rules.",
    KreateTaskGroup.ASSURANCE
) {
    @get:Internal
    @get:Option(option = "rule", description = "The rule, advisory or standard to explain.")
    public abstract val rule: Property<String>

    @TaskAction
    public fun explain() {
        val raw = rule.orNull?.trim() ?: return list()
        val standard = StandardIdGrammar.parse(raw.lowercase())
        if (standard != null) return logger.lifecycle(describe(standard))
        val id = raw.uppercase()
        val known = RuleCatalog.find(id)
        if (known != null) return logger.lifecycle(describe(known))
        if (ADVISORY_ID.matches(id)) return logger.lifecycle("$id is an advisory. Its record: $ADVISORY_URL$id")
        val similar = similarRules(id)
        val hint = similar.takeIf { names -> names.isNotEmpty() }?.let { names ->
            " Did you mean ${names.joinToString()}?"
        }
        val unknown = "No rule ${SafeText.quoted(id)} is known."
        throw InvalidUserDataException("$unknown${hint.orEmpty()} Run without --rule to list all rules.")
    }

    private fun similarRules(id: String): List<String> {
        val ids = RuleCatalog.rules.map { candidate -> candidate.id }
        val containing = ids.filter { candidate -> candidate.contains(id) }
        val close = closeMatches(id, ids, MAX_SUGGESTIONS, ignoreCase = true)
        val similar = containing + close
        return similar.distinct()
    }

    private fun list() {
        val bySource = RuleCatalog.rules.groupBy { rule -> rule.source.id }
        bySource.forEach { (source, rules) ->
            logger.lifecycle("$source:")
            rules.forEach { rule -> logger.lifecycle(lineOf(rule)) }
        }
    }

    private fun lineOf(rule: RuleInfo): String {
        val severity = rule.severity.name
        return "  ${rule.id.padEnd(ID_WIDTH)} ${severity.padEnd(SEVERITY_WIDTH)} ${rule.summary}"
    }

    private fun describe(standard: StandardId): String {
        val page = StandardLinks.infoUrlOf(standard)?.let { url -> "Its page: $url" } ?: "It has no page Kreate knows."
        return listOf(
            "$standard is a standard of the kind ${standard.kind.id}.",
            page,
            "Record a review: ./gradlew ${StandardsTaskNames.REVIEW} --standard=$standard --note=\"...\"",
            "Accept its findings: assurance { ignore(\"$standard\") { reason = \"...\" } }"
        ).joinToString("\n")
    }

    private fun describe(rule: RuleInfo): String = listOf(
        "${rule.id} (${rule.severity.name}, ${rule.source.id}/${rule.category})",
        rule.summary,
        "",
        rule.explanation
    ).joinToString("\n")

    private companion object {
        const val ID_WIDTH: Int = 30

        const val SEVERITY_WIDTH: Int = 8

        const val MAX_SUGGESTIONS: Int = 3
    }
}
