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
package com.davils.kreate.assurance

import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardId
import com.davils.kreate.standard.model.StandardIds
import com.davils.kreate.standard.model.SuiteSource
import org.gradle.api.Action
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import java.time.Duration
import javax.inject.Inject

/**
 * Configuration of the standards a project implements, such as RFCs, ISO and DIN standards, W3C,
 * WHATWG and Unicode specifications, the test suites it vendors, and of the checks that report
 * when one of them changes.
 *
 * Every declared standard has to be reviewed once with `kreateStandardsReview`, which records what
 * the issuing body publishes about it in [lockFile]. `kreateStandardsAudit` compares that record
 * with the body every time it runs and reports a standard that was obsoleted, updated, revised or
 * corrected since; `kreateStandardsInventory` reports a standard that was never reviewed, a review
 * that is due and a standard the code names without declaring it.
 *
 * @param factory The object factory used to create the properties.
 * @param layout The layout of the project, which locates the default lock file.
 * @since 5.3.0
 */
public abstract class AssuranceStandardsExtension @Inject constructor(factory: ObjectFactory, layout: ProjectLayout) {
    private val standards: MutableList<StandardDeclaration> = mutableListOf()

    /**
     * Whether the standards are checked. Defaults to `false`.
     *
     * @since 5.3.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The lock file that records the reviewed state of every standard. Defaults to
     * `standards.lock` in the project directory. Commit it; only `kreateStandardsReview` writes it.
     *
     * @since 5.3.0
     */
    public val lockFile: RegularFileProperty = factory.fileProperty().convention(
        layout.projectDirectory.file("standards.lock")
    )

    /**
     * How long a review stands before it is due again, for every standard that sets no interval
     * of its own. Absent by default, so only the standards with an interval of their own fall due.
     *
     * @since 5.3.0
     */
    public val reviewEvery: Property<Duration> = factory.property(Duration::class.java)

    /**
     * Configuration of the search for standards the code names without declaring them.
     *
     * @since 5.3.0
     */
    @get:Nested
    public abstract val inventory: AssuranceStandardsInventoryExtension

    /**
     * Configures the search for standards the code names without declaring them.
     *
     * @param action The configuration action.
     * @since 5.3.0
     */
    public fun inventory(action: Action<AssuranceStandardsInventoryExtension>) {
        action.execute(inventory)
    }

    /**
     * Declares an RFC of the IETF, checked through the RFC Editor together with its errata.
     *
     * @param number The number of the RFC, such as `9562`.
     * @throws IllegalArgumentException If [number] is no RFC number.
     * @since 5.3.0
     */
    public fun rfc(number: Int) {
        rfc(number) { }
    }

    /**
     * Declares an RFC of the IETF, checked through the RFC Editor together with its errata.
     *
     * @param number The number of the RFC, such as `9562`.
     * @param action The configuration of the standard.
     * @throws IllegalArgumentException If [number] is no RFC number.
     * @since 5.3.0
     */
    public fun rfc(number: Int, action: Action<AssuranceStandardSpec>) {
        declare(StandardIds.rfc(number), action)
    }

    /**
     * Declares an Internet-Draft, checked through the IETF Datatracker.
     *
     * @param name The name of the draft without its revision, such as
     * `draft-ietf-httpbis-resumable-upload`.
     * @throws IllegalArgumentException If [name] is no name of a draft.
     * @since 5.3.0
     */
    public fun draft(name: String) {
        draft(name) { }
    }

    /**
     * Declares an Internet-Draft, checked through the IETF Datatracker.
     *
     * @param name The name of the draft without its revision, such as
     * `draft-ietf-httpbis-resumable-upload`.
     * @param action The configuration of the standard.
     * @throws IllegalArgumentException If [name] is no name of a draft.
     * @since 5.3.0
     */
    public fun draft(name: String, action: Action<AssuranceStandardSpec>) {
        declare(StandardIds.draft(name), action)
    }

    /**
     * Declares an ISO or ISO/IEC standard, checked through the open data of ISO.
     *
     * @param reference The reference without its year, such as `ISO 8601-1` or `ISO/IEC 27001`.
     * @throws IllegalArgumentException If [reference] is no reference of an ISO standard.
     * @since 5.3.0
     */
    public fun iso(reference: String) {
        iso(reference) { }
    }

    /**
     * Declares an ISO or ISO/IEC standard, checked through the open data of ISO.
     *
     * @param reference The reference without its year, such as `ISO 8601-1` or `ISO/IEC 27001`.
     * @param action The configuration of the standard.
     * @throws IllegalArgumentException If [reference] is no reference of an ISO standard.
     * @since 5.3.0
     */
    public fun iso(reference: String, action: Action<AssuranceStandardSpec>) {
        declare(StandardIds.iso(reference), action)
    }

    /**
     * Declares a specification of the W3C, checked through the W3C API together with its errata.
     *
     * @param shortname The short name of the specification, such as `webauthn-3`.
     * @throws IllegalArgumentException If [shortname] is no short name of a specification.
     * @since 5.3.0
     */
    public fun w3c(shortname: String) {
        w3c(shortname) { }
    }

    /**
     * Declares a specification of the W3C, checked through the W3C API together with its errata.
     *
     * @param shortname The short name of the specification, such as `webauthn-3`.
     * @param action The configuration of the standard.
     * @throws IllegalArgumentException If [shortname] is no short name of a specification.
     * @since 5.3.0
     */
    public fun w3c(shortname: String, action: Action<AssuranceStandardSpec>) {
        declare(StandardIds.w3c(shortname), action)
    }

    /**
     * Declares a living standard of the WHATWG, checked through its review drafts.
     *
     * @param name The name of the standard, such as `url` or `fetch`.
     * @throws IllegalArgumentException If [name] is no name of a WHATWG standard.
     * @since 5.3.0
     */
    public fun whatwg(name: String) {
        whatwg(name) { }
    }

    /**
     * Declares a living standard of the WHATWG, checked through its review drafts.
     *
     * @param name The name of the standard, such as `url` or `fetch`.
     * @param action The configuration of the standard.
     * @throws IllegalArgumentException If [name] is no name of a WHATWG standard.
     * @since 5.3.0
     */
    public fun whatwg(name: String, action: Action<AssuranceStandardSpec>) {
        declare(StandardIds.whatwg(name), action)
    }

    /**
     * Declares the Unicode Character Database, checked for new versions of Unicode.
     *
     * @since 5.3.0
     */
    public fun unicode() {
        unicode { }
    }

    /**
     * Declares the Unicode Character Database, checked for new versions of Unicode.
     *
     * @param action The configuration of the standard.
     * @since 5.3.0
     */
    public fun unicode(action: Action<AssuranceStandardSpec>) {
        declare(StandardIds.unicode(null), action)
    }

    /**
     * Declares a Unicode Standard Annex or Technical Standard, checked for new revisions.
     *
     * @param report The report, such as `UAX #31` or `UTS #39`.
     * @throws IllegalArgumentException If [report] names no annex or technical standard.
     * @since 5.3.0
     */
    public fun unicode(report: String) {
        unicode(report) { }
    }

    /**
     * Declares a Unicode Standard Annex or Technical Standard, checked for new revisions.
     *
     * @param report The report, such as `UAX #31` or `UTS #39`.
     * @param action The configuration of the standard.
     * @throws IllegalArgumentException If [report] names no annex or technical standard.
     * @since 5.3.0
     */
    public fun unicode(report: String, action: Action<AssuranceStandardSpec>) {
        declare(StandardIds.unicode(report), action)
    }

    /**
     * Declares a specification published as a GitHub repository, such as ECMA-262, YAML or
     * Semantic Versioning, checked for new releases or tags.
     *
     * @param repository The repository, such as `yaml/yaml-spec`.
     * @throws IllegalArgumentException If [repository] is no repository of the form `owner/name`.
     * @since 5.3.0
     */
    public fun github(repository: String) {
        github(repository) { }
    }

    /**
     * Declares a specification published as a GitHub repository, such as ECMA-262, YAML or
     * Semantic Versioning, checked for new releases or tags.
     *
     * @param repository The repository, such as `yaml/yaml-spec`.
     * @param action The configuration of the standard, such as the tags that count.
     * @throws IllegalArgumentException If [repository] is no repository of the form `owner/name`.
     * @since 5.3.0
     */
    public fun github(repository: String, action: Action<AssuranceStandardSpec>) {
        declare(StandardIds.github(repository), action)
    }

    /**
     * Declares a DIN standard that falls due after [reviewEvery], because no service reports
     * changes of DIN standards.
     *
     * @param number The number of the standard, such as `DIN 5008` or `DIN EN ISO 9001`.
     * @throws IllegalArgumentException If [number] is no number of a DIN standard.
     * @since 5.3.0
     */
    public fun din(number: String) {
        din(number) { }
    }

    /**
     * Declares a DIN standard. No service reports changes of DIN standards, so it needs a
     * [AssuranceStandardSpec.reviewEvery], and its [AssuranceStandardSpec.url] can be watched.
     *
     * @param number The number of the standard, such as `DIN 5008` or `DIN EN ISO 9001`.
     * @param action The configuration of the standard.
     * @throws IllegalArgumentException If [number] is no number of a DIN standard.
     * @since 5.3.0
     */
    public fun din(number: String, action: Action<AssuranceStandardSpec>) {
        declare(StandardIds.din(number), action)
    }

    /**
     * Declares any other standard, such as one of ECMA or OASIS, that falls due after
     * [reviewEvery], because no service reports its changes.
     *
     * @param name The name of the standard, such as `ECMA-404`.
     * @throws IllegalArgumentException If [name] gives no name.
     * @since 5.3.0
     */
    public fun document(name: String) {
        document(name) { }
    }

    /**
     * Declares any other standard, such as one of ECMA or OASIS. No service reports its changes,
     * so it needs a [AssuranceStandardSpec.reviewEvery], and its [AssuranceStandardSpec.url] can
     * be watched.
     *
     * @param name The name of the standard, such as `ECMA-404`.
     * @param action The configuration of the standard.
     * @throws IllegalArgumentException If [name] gives no name.
     * @since 5.3.0
     */
    public fun document(name: String, action: Action<AssuranceStandardSpec>) {
        declare(StandardIds.document(name), action)
    }

    /**
     * Declares a test suite the project vendors, such as the W3C XML Conformance Test Suite, taken
     * from an archive pinned by its SHA-256 or from a GitHub repository pinned by a commit. The
     * audit checks that the vendored files are unchanged and byte for byte those of the pinned
     * archive or commit, and reports newer versions upstream.
     *
     * @param name The name of the suite, such as `xmlconf`.
     * @param action The configuration of the suite: where it comes from and where it is vendored.
     * @throws IllegalArgumentException If [name] gives no name.
     * @since 5.4.0
     */
    public fun suite(name: String, action: Action<AssuranceStandardSuiteSpec>) {
        val spec = AssuranceStandardSuiteSpec()
        action.execute(spec)
        val source = SuiteSource(
            vendored = spec.vendored?.trim(),
            archive = spec.archive?.trim(),
            sha256 = spec.sha256?.trim()?.lowercase(),
            github = spec.github?.trim()?.lowercase(),
            commit = spec.commit?.trim()?.lowercase(),
            watch = spec.watch?.trim(),
            root = spec.root?.trim()
        )
        val id = StandardIds.suite(name)
        standards += StandardDeclaration(id, spec.note, spec.reviewEvery?.seconds, suite = source)
    }

    internal fun declaredStandards(): List<StandardDeclaration> = standards.toList()

    private fun declare(id: StandardId, action: Action<AssuranceStandardSpec>) {
        val spec = AssuranceStandardSpec()
        action.execute(spec)
        val seconds = spec.reviewEvery?.seconds
        standards += StandardDeclaration(id, spec.note, seconds, spec.tags, spec.url, spec.errata?.trim())
    }
}
