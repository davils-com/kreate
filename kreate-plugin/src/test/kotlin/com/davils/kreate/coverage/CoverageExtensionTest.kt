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

package com.davils.kreate.coverage

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreatePlugin
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit as KoverCoverageUnit

class CoverageExtensionTest : FunSpec({
    fun coverage(): CoverageExtension {
        val builder = ProjectBuilder.builder()
        val project: Project = builder.withName("sample").build()
        project.pluginManager.apply(KreatePlugin::class.java)
        return project.extensions.getByType(KreateExtension::class.java).project.coverage
    }

    context("CoverageExtension") {
        context("defaults") {
            test("the integration is off until it is asked for") {
                coverage().enabled.get() shouldBe false
            }

            test("measurement uses Kover's own engine rather than JaCoCo") {
                val extension = coverage()

                extension.useJacoco.get() shouldBe false
                extension.jacocoVersion.isPresent shouldBe false
            }

            test("no report is generated during check") {
                val reports = coverage().reports

                reports.xml.onCheck.get() shouldBe false
                reports.html.onCheck.get() shouldBe false
                reports.log.onCheck.get() shouldBe false
                reports.binary.onCheck.get() shouldBe false
            }

            test("verification does run during check, and fails rather than warns") {
                val verify = coverage().verify

                verify.runOnCheck.get() shouldBe true
                verify.warningInsteadOfFailure.get() shouldBe false
            }

            test("no coverage bound is set") {
                val verify = coverage().verify

                verify.minLineCoverage.isPresent shouldBe false
                verify.minBranchCoverage.isPresent shouldBe false
                verify.minInstructionCoverage.isPresent shouldBe false
            }

            test("no named rules are registered") {
                coverage().verify.rules.isEmpty() shouldBe true
            }

            test("aggregation is off, and names no projects") {
                val aggregate = coverage().aggregate

                aggregate.enabled.get() shouldBe false
                aggregate.projects.get() shouldBe emptyList()
            }

            test("the log format matches the documented GitLab coverage expression") {
                coverage().reports.log.format.get() shouldBe "<entity> line coverage: <value>%"
            }

            test("reports land where the documentation says they do") {
                val reports = coverage().reports

                reports.xml.file.get().asFile.invariantSeparatorsPath shouldEndWith
                    "reports/kover/report.xml"
                reports.html.directory.get().asFile.invariantSeparatorsPath shouldEndWith
                    "reports/kover/html"
            }

            test("nothing is filtered, excluded or left uninstrumented") {
                val extension = coverage()

                extension.filters.excludes.classes.get() shouldBe emptyList()
                extension.filters.includes.classes.get() shouldBe emptyList()
                extension.sources.excludedSourceSets.get() shouldBe emptyList()
                extension.instrumentation.excludedClasses.get() shouldBe emptyList()
                extension.instrumentation.disabledForAll.get() shouldBe false
            }
        }

        context("named rules") {
            test("a rule keeps the name it was registered under") {
                val verify = coverage().verify
                verify.rules.create("No untested class")

                verify.rules.getByName("No untested class").name shouldBe "No untested class"
            }

            test("a rule inherits the grouping unless it sets one") {
                val rule = coverage().verify.rules.create("Rule")

                rule.groupBy.isPresent shouldBe false
                rule.disabled.get() shouldBe false
                rule.bounds.get() shouldBe emptyList()
            }

            test("minBound records the value, unit and aggregation it was given") {
                val rule = coverage().verify.rules.create("Rule")
                rule.minBound(70, CoverageUnit.BRANCH, Aggregation.COVERED_PERCENTAGE)

                val bound = rule.bounds.get().single()
                bound.min.get() shouldBe 70
                bound.max.isPresent shouldBe false
                bound.unit.get() shouldBe CoverageUnit.BRANCH
                bound.aggregation.get() shouldBe Aggregation.COVERED_PERCENTAGE
            }

            test("maxBound caps rather than floors") {
                val rule = coverage().verify.rules.create("Rule")
                rule.maxBound(100, CoverageUnit.LINE, Aggregation.MISSED_COUNT)

                val bound = rule.bounds.get().single()
                bound.max.get() shouldBe 100
                bound.min.isPresent shouldBe false
                bound.aggregation.get() shouldBe Aggregation.MISSED_COUNT
            }

            test("a rule accumulates every bound added to it") {
                val rule = coverage().verify.rules.create("Rule")
                rule.minBound(80, CoverageUnit.LINE)
                rule.minBound(70, CoverageUnit.BRANCH)

                rule.bounds.get().size shouldBe 2
            }
        }

        context("configuration blocks") {
            test("the sources, instrumentation and filter blocks configure their settings") {
                val extension = coverage()
                extension.sources { excludeJava.set(true) }
                extension.instrumentation { excludedClasses.add("com.acme.Generated") }
                extension.filters {
                    excludes { packages.add("com.acme.generated") }
                    includes { classes.add("com.acme.*") }
                }

                extension.sources.excludeJava.get() shouldBe true
                extension.instrumentation.excludedClasses.get() shouldBe listOf("com.acme.Generated")
                extension.filters.excludes.packages.get() shouldBe listOf("com.acme.generated")
                extension.filters.includes.classes.get() shouldBe listOf("com.acme.*")
            }

            test("the reports block configures every report") {
                val extension = coverage()
                extension.reports {
                    xml { onCheck.set(true) }
                    html { title.set("Coverage") }
                    log { header.set("Coverage summary") }
                    binary { onCheck.set(true) }
                }
                val reports = extension.reports

                reports.xml.onCheck.get() shouldBe true
                reports.html.title.get() shouldBe "Coverage"
                reports.log.header.get() shouldBe "Coverage summary"
                reports.binary.onCheck.get() shouldBe true
            }

            test("the verify and aggregate blocks configure their settings") {
                val extension = coverage()
                extension.verify {
                    minLineCoverage.set(88)
                    rules { create("Branches") }
                }
                extension.aggregate { enabled.set(true) }

                extension.verify.minLineCoverage.get() shouldBe 88
                extension.verify.rules.names shouldBe setOf("Branches")
                extension.aggregate.enabled.get() shouldBe true
            }

            test("a bound block adds a fully configured bound to the rule") {
                val rule = coverage().verify.rules.create("Rule")
                rule.bound {
                    min.set(60)
                    unit.set(CoverageUnit.INSTRUCTION)
                }

                val bound = rule.bounds.get().single()
                bound.min.get() shouldBe 60
                bound.unit.get() shouldBe CoverageUnit.INSTRUCTION
            }
        }

        context("mapping onto Kover") {
            test("every coverage unit maps to a Kover unit of the same name") {
                CoverageUnit.entries.forEach { unit ->
                    KoverCoverageUnit.valueOf(unit.name).name shouldBe unit.name
                }
            }

            test("every aggregation maps to a Kover aggregation type of the same name") {
                Aggregation.entries.forEach { aggregation ->
                    AggregationType.valueOf(aggregation.name).name shouldBe aggregation.name
                }
            }

            test("every grouping maps to a Kover grouping entity of the same name") {
                Grouping.entries.forEach { grouping ->
                    GroupingEntityType.valueOf(grouping.name).name shouldBe grouping.name
                }
            }
        }
    }
})
