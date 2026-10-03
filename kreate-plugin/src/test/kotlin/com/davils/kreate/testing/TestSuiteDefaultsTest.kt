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

package com.davils.kreate.testing

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreatePlugin
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class TestSuiteDefaultsTest : FunSpec({

    fun tests(): TestsExtension {
        val builder = ProjectBuilder.builder()
        val project: Project = builder.withName("sample").build()
        project.pluginManager.apply(KreatePlugin::class.java)
        return project.extensions.getByType(KreateExtension::class.java).project.tests
    }

    fun unitSuite() = tests().suites.getByName(TestSuiteNames.UNIT)

    fun integrationSuite() = tests().suites.getByName(TestSuiteNames.INTEGRATION)

    context("test suites") {

        context("registration") {

            test("a unit and an integration suite exist without being registered") {
                tests().suites.names shouldBe setOf(TestSuiteNames.UNIT, TestSuiteNames.INTEGRATION)
            }

            test("both default suites are enabled") {
                val suites = tests().suites
                suites.getByName(TestSuiteNames.UNIT).enabled.get() shouldBe true
                suites.getByName(TestSuiteNames.INTEGRATION).enabled.get() shouldBe true
            }

            test("a suite's source set is named after the suite") {
                val integration = integrationSuite()
                integration.sourceSetName.get() shouldBe "integrationTest"
            }
        }

        context("check wiring") {

            test("check runs the unit suite") {
                unitSuite().runOnCheck.get() shouldBe true
            }

            test("check does not run the integration suite, which may need Docker") {
                integrationSuite().runOnCheck.get() shouldBe false
            }

            test("the integration suite is ordered after the unit suite") {
                val integration = integrationSuite()
                integration.mustRunAfterSuites.get() shouldBe listOf(TestSuiteNames.UNIT)
            }

            test("a suite sees main's internal declarations by default") {
                unitSuite().associateWithMain.get() shouldBe true
            }
        }

        context("legacy policy") {

            test("the conventional test source set is disabled rather than failed on") {
                tests().legacyTestSourceSet.get() shouldBe LegacyTestPolicy.DISABLE
            }

            test("disabling the legacy task clears its source directories") {
                tests().legacySourceDirectories.get() shouldBe LegacySourceDirectories.CLEAR
            }

            test("aliasing the legacy task adopts its sources, so nothing has to move") {
                val tests = tests()
                tests.legacyTestSourceSet.set(LegacyTestPolicy.ALIAS)
                tests.legacySourceDirectories.get() shouldBe LegacySourceDirectories.ADOPT
            }

            test("keeping the legacy task keeps its sources") {
                val tests = tests()
                tests.legacyTestSourceSet.set(LegacyTestPolicy.KEEP)
                tests.legacySourceDirectories.get() shouldBe LegacySourceDirectories.KEEP
            }

            test("an explicit source directory mode wins over the one the policy implies") {
                val tests = tests()
                tests.legacySourceDirectories.set(LegacySourceDirectories.KEEP)
                tests.legacyTestSourceSet.set(LegacyTestPolicy.DISABLE)
                tests.legacySourceDirectories.get() shouldBe LegacySourceDirectories.KEEP
            }
        }

        context("inheritance") {

            test("a suite takes the enclosing block's parallelism") {
                val tests = tests()
                tests.maxParallelForks.set(9)
                tests.suites.getByName(TestSuiteNames.UNIT).maxParallelForks.get() shouldBe 9
            }

            test("a suite's own value wins over the enclosing block's") {
                val tests = tests()
                tests.maxParallelForks.set(9)
                val integration = tests.suites.getByName(TestSuiteNames.INTEGRATION)
                integration.maxParallelForks.set(1)

                integration.maxParallelForks.get() shouldBe 1
                tests.suites.getByName(TestSuiteNames.UNIT).maxParallelForks.get() shouldBe 9
            }

            test("a suite takes the enclosing block's timeout") {
                val tests = tests()
                tests.timeoutMinutes.set(42L)
                tests.suites.getByName(TestSuiteNames.UNIT).timeoutMinutes.get() shouldBe 42L
            }

            test("a suite takes the enclosing block's logging settings") {
                val tests = tests()
                tests.logging.logPassedTests.set(false)
                tests.suites.getByName(TestSuiteNames.UNIT).logging.logPassedTests.get() shouldBe false
            }

            test("a suite's own logging setting wins over the enclosing block's") {
                val tests = tests()
                tests.logging.logPassedTests.set(false)
                val unit = tests.suites.getByName(TestSuiteNames.UNIT)
                unit.logging.logPassedTests.set(true)

                unit.logging.logPassedTests.get() shouldBe true
            }

            test("a suite takes the enclosing block's report settings") {
                val tests = tests()
                tests.report.enabled.set(true)
                tests.suites.getByName(TestSuiteNames.INTEGRATION).report.enabled.get() shouldBe true
            }

            test("a suite takes the enclosing block's Kotest settings") {
                val tests = tests()
                tests.kotest.enabled.set(true)
                tests.kotest.version.set("9.9.9")
                val unit = tests.suites.getByName(TestSuiteNames.UNIT)

                unit.kotest.enabled.get() shouldBe true
                unit.kotest.version.get() shouldBe "9.9.9"
            }

            test("a registered suite inherits just like a pre-registered one") {
                val tests = tests()
                tests.timeoutMinutes.set(7L)
                tests.suites.register("contractTest")

                tests.suites.getByName("contractTest").timeoutMinutes.get() shouldBe 7L
            }
        }

        context("Kotest bundle") {

            test("the bundle is off, so a project that brings its own framework is untouched") {
                tests().kotest.enabled.get() shouldBe false
            }

            test("the bundle carries the matchers and nothing else") {
                tests().kotest.modules.get() shouldBe listOf(KotestModule.ASSERTIONS)
            }

            test("the JUnit Platform launcher is added, because a suite cannot start without it") {
                tests().kotest.addJUnitPlatformLauncher.get() shouldBe true
            }
        }

        context("coverage") {

            test("suite sources are kept out of the coverage denominator") {
                tests().excludeSuitesFromCoverage.get() shouldBe true
            }
        }
    }
})
