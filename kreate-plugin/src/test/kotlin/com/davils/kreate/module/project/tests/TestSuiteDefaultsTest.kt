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

package com.davils.kreate.module.project.tests

import com.davils.kreate.Kreate
import com.davils.kreate.KreateExtension
import com.davils.kreate.KreateTasks
import com.davils.kreate.module.project.tests.suite.KotestModule
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for the test suite DSL defaults and for the inheritance between the `tests { }` block
 * and its suites.
 *
 * These are contract tests. A suite's defaults decide what runs on `check` in every project
 * that did not spell the answer out, and the inheritance decides whether a value set once is
 * actually picked up - both of which change behaviour silently when they regress.
 */
@DisplayName("test suites")
class TestSuiteDefaultsTest {

    private fun tests(): TestsExtension {
        val project: Project = ProjectBuilder.builder().withName("sample").build()
        project.pluginManager.apply(Kreate::class.java)
        return project.extensions.getByType(KreateExtension::class.java).project.tests
    }

    @Nested
    @DisplayName("registration")
    inner class Registration {

        @Test
        @DisplayName("a unit and an integration suite exist without being registered")
        fun defaultSuites() {
            tests().suites.names shouldBe setOf(KreateTasks.Tests.UNIT, KreateTasks.Tests.INTEGRATION)
        }

        @Test
        @DisplayName("both default suites are enabled")
        fun defaultSuitesEnabled() {
            val suites = tests().suites
            suites.getByName(KreateTasks.Tests.UNIT).enabled.get() shouldBe true
            suites.getByName(KreateTasks.Tests.INTEGRATION).enabled.get() shouldBe true
        }

        @Test
        @DisplayName("a suite's source set is named after the suite")
        fun sourceSetName() {
            val integration = tests().suites.getByName(KreateTasks.Tests.INTEGRATION)
            integration.sourceSetName.get() shouldBe "integrationTest"
        }
    }

    @Nested
    @DisplayName("check wiring")
    inner class CheckWiring {

        @Test
        @DisplayName("check runs the unit suite")
        fun unitOnCheck() {
            tests().suites.getByName(KreateTasks.Tests.UNIT).runOnCheck.get() shouldBe true
        }

        @Test
        @DisplayName("check does not run the integration suite, which may need Docker")
        fun integrationOffCheck() {
            tests().suites.getByName(KreateTasks.Tests.INTEGRATION).runOnCheck.get() shouldBe false
        }

        @Test
        @DisplayName("the integration suite is ordered after the unit suite")
        fun integrationAfterUnit() {
            val integration = tests().suites.getByName(KreateTasks.Tests.INTEGRATION)
            integration.mustRunAfterSuites.get() shouldBe listOf(KreateTasks.Tests.UNIT)
        }

        @Test
        @DisplayName("a suite sees main's internal declarations by default")
        fun associatedWithMain() {
            tests().suites.getByName(KreateTasks.Tests.UNIT).associateWithMain.get() shouldBe true
        }
    }

    @Nested
    @DisplayName("legacy policy")
    inner class Legacy {

        @Test
        @DisplayName("the conventional test source set is disabled rather than failed on")
        fun defaultPolicy() {
            tests().legacyTestSourceSet.get() shouldBe LegacyTestPolicy.DISABLE
        }

        @Test
        @DisplayName("disabling the legacy task clears its source directories")
        fun disableClears() {
            tests().legacySourceDirectories.get() shouldBe LegacySourceDirectories.CLEAR
        }

        @Test
        @DisplayName("aliasing the legacy task adopts its sources, so nothing has to move")
        fun aliasAdopts() {
            val tests = tests()
            tests.legacyTestSourceSet.set(LegacyTestPolicy.ALIAS)
            tests.legacySourceDirectories.get() shouldBe LegacySourceDirectories.ADOPT
        }

        @Test
        @DisplayName("keeping the legacy task keeps its sources")
        fun keepKeeps() {
            val tests = tests()
            tests.legacyTestSourceSet.set(LegacyTestPolicy.KEEP)
            tests.legacySourceDirectories.get() shouldBe LegacySourceDirectories.KEEP
        }

        @Test
        @DisplayName("an explicit source directory mode wins over the one the policy implies")
        fun explicitWins() {
            val tests = tests()
            tests.legacySourceDirectories.set(LegacySourceDirectories.KEEP)
            tests.legacyTestSourceSet.set(LegacyTestPolicy.DISABLE)
            tests.legacySourceDirectories.get() shouldBe LegacySourceDirectories.KEEP
        }
    }

    @Nested
    @DisplayName("inheritance")
    inner class Inheritance {

        @Test
        @DisplayName("a suite takes the enclosing block's parallelism")
        fun inheritsParallelism() {
            val tests = tests()
            tests.maxParallelForks.set(9)
            tests.suites.getByName(KreateTasks.Tests.UNIT).maxParallelForks.get() shouldBe 9
        }

        @Test
        @DisplayName("a suite's own value wins over the enclosing block's")
        fun overrideWins() {
            val tests = tests()
            tests.maxParallelForks.set(9)
            val integration = tests.suites.getByName(KreateTasks.Tests.INTEGRATION)
            integration.maxParallelForks.set(1)

            integration.maxParallelForks.get() shouldBe 1
            tests.suites.getByName(KreateTasks.Tests.UNIT).maxParallelForks.get() shouldBe 9
        }

        @Test
        @DisplayName("a suite takes the enclosing block's timeout")
        fun inheritsTimeout() {
            val tests = tests()
            tests.timeoutMinutes.set(42L)
            tests.suites.getByName(KreateTasks.Tests.UNIT).timeoutMinutes.get() shouldBe 42L
        }

        @Test
        @DisplayName("a suite takes the enclosing block's logging settings")
        fun inheritsLogging() {
            val tests = tests()
            tests.logging.logPassedTests.set(false)
            tests.suites.getByName(KreateTasks.Tests.UNIT).logging.logPassedTests.get() shouldBe false
        }

        @Test
        @DisplayName("a suite's own logging setting wins over the enclosing block's")
        fun overridesLogging() {
            val tests = tests()
            tests.logging.logPassedTests.set(false)
            val unit = tests.suites.getByName(KreateTasks.Tests.UNIT)
            unit.logging.logPassedTests.set(true)

            unit.logging.logPassedTests.get() shouldBe true
        }

        @Test
        @DisplayName("a suite takes the enclosing block's report settings")
        fun inheritsReport() {
            val tests = tests()
            tests.report.enabled.set(true)
            tests.suites.getByName(KreateTasks.Tests.INTEGRATION).report.enabled.get() shouldBe true
        }

        @Test
        @DisplayName("a suite takes the enclosing block's Kotest settings")
        fun inheritsKotest() {
            val tests = tests()
            tests.kotest.enabled.set(true)
            tests.kotest.version.set("9.9.9")
            val unit = tests.suites.getByName(KreateTasks.Tests.UNIT)

            unit.kotest.enabled.get() shouldBe true
            unit.kotest.version.get() shouldBe "9.9.9"
        }

        @Test
        @DisplayName("a registered suite inherits just like a pre-registered one")
        fun registeredSuiteInherits() {
            val tests = tests()
            tests.timeoutMinutes.set(7L)
            tests.suites.register("contractTest")

            tests.suites.getByName("contractTest").timeoutMinutes.get() shouldBe 7L
        }
    }

    @Nested
    @DisplayName("Kotest bundle")
    inner class Kotest {

        @Test
        @DisplayName("the bundle is off, so a project that brings its own framework is untouched")
        fun disabledByDefault() {
            tests().kotest.enabled.get() shouldBe false
        }

        @Test
        @DisplayName("the bundle carries the matchers and nothing else")
        fun defaultModules() {
            tests().kotest.modules.get() shouldBe listOf(KotestModule.ASSERTIONS)
        }

        @Test
        @DisplayName("the JUnit Platform launcher is added, because a suite cannot start without it")
        fun launcherOn() {
            // Gradle adds a launcher only to the `test` suite it created itself, so a suite's
            // task would fail before running anything.
            tests().kotest.addJUnitPlatformLauncher.get() shouldBe true
        }
    }

    @Nested
    @DisplayName("coverage")
    inner class Coverage {

        @Test
        @DisplayName("suite sources are kept out of the coverage denominator")
        fun excludedByDefault() {
            tests().excludeSuitesFromCoverage.get() shouldBe true
        }
    }
}
