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

package com.davils.kreate.testing.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreatePlugin
import com.davils.kreate.freshDirectory
import com.davils.kreate.testing.KotestModule
import com.davils.kreate.testing.TestSuiteNames
import com.davils.kreate.testing.TestsExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder

private const val LIBRARY = "com.acme:library:1.0.0"
private const val ANNOTATIONS = "com.acme:annotations:1.0.0"
private const val DRIVER = "com.acme:driver:1.0.0"
private const val BOM = "com.acme:bom:1.0.0"

class SuiteDependenciesTest : FunSpec({
    val workspace = tempdir()

    fun evaluated(configure: (TestsExtension) -> Unit): Project {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply(KreatePlugin::class.java)
        val kreate = project.extensions.getByType(KreateExtension::class.java)
        val tests = kreate.project.tests
        tests.enabled.set(true)
        configure(tests)
        (project as ProjectInternal).evaluate()
        return project
    }

    fun declaredIn(project: Project, configurationName: String): List<String> {
        val configuration = project.configurations.getByName(configurationName)
        return configuration.dependencies.map { dependency -> "${dependency.group}:${dependency.name}" }
    }

    context("suite dependencies") {
        test("adds every declared dependency to the suite's own configurations") {
            val project = evaluated { tests ->
                tests.suites.getByName(TestSuiteNames.UNIT).dependencies {
                    implementation(LIBRARY)
                    compileOnly(ANNOTATIONS)
                    runtimeOnly(DRIVER)
                    platform(BOM)
                }
            }

            declaredIn(project, "unitTestImplementation") shouldContain "com.acme:library"
            declaredIn(project, "unitTestImplementation") shouldContain "com.acme:bom"
            declaredIn(project, "unitTestCompileOnly") shouldContain "com.acme:annotations"
            declaredIn(project, "unitTestRuntimeOnly") shouldContain "com.acme:driver"
        }

        test("adds the Kotest bundle when the suite asks for it") {
            val project = evaluated { tests ->
                tests.kotest {
                    enabled.set(true)
                    modules.set(listOf(KotestModule.ASSERTIONS, KotestModule.PROPERTY))
                }
            }
            val implementation = declaredIn(project, "unitTestImplementation")

            implementation shouldContain "io.kotest:kotest-runner-junit5"
            implementation shouldContain "io.kotest:kotest-property"
        }

        test("leaves the JUnit Platform launcher out when the suite declines it") {
            val project = evaluated { tests ->
                tests.suites.getByName(TestSuiteNames.UNIT).kotest { addJUnitPlatformLauncher.set(false) }
            }

            declaredIn(project, "unitTestRuntimeOnly") shouldNotContain "org.junit.platform:junit-platform-launcher"
            declaredIn(project, "integrationTestRuntimeOnly") shouldContain
                "org.junit.platform:junit-platform-launcher"
        }
    }
})
