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

package com.davils.kreate.configuration.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreatePlugin
import com.davils.kreate.configuration.ConfigurationSchemaExtension
import com.davils.kreate.configuration.ConfigurationSchemaTaskNames
import com.davils.kreate.configuration.task.ConfigSchemaCheck
import com.davils.kreate.configuration.task.ConfigSchemaDump
import com.davils.kreate.configuration.task.ConfigValidate
import com.davils.kreate.freshDirectory
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.TaskInternal
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

private const val HOLDER = "com.acme.ConfigurationKt"

class ConfigurationSchemaWiringTest : FunSpec({
    val workspace = tempdir()

    fun projectWith(pluginId: String): Project {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        project.pluginManager.apply(pluginId)
        project.pluginManager.apply(KreatePlugin::class.java)
        return project
    }

    fun schemaOf(project: Project): ConfigurationSchemaExtension {
        val kreate = project.extensions.getByType(KreateExtension::class.java)
        return kreate.project.configurationSchema
    }

    fun evaluated(project: Project, configure: (ConfigurationSchemaExtension) -> Unit): Project {
        val schema = schemaOf(project)
        schema.enabled.set(true)
        configure(schema)
        (project as ProjectInternal).evaluate()
        return project
    }

    fun checkDependenciesOf(project: Project): List<String> {
        val check = project.tasks.getByName(LifecycleBasePlugin.CHECK_TASK_NAME)
        val providers = check.dependsOn.filterIsInstance<TaskProvider<*>>()
        return providers.map { provider -> provider.name }
    }

    fun classpathSourcesOf(project: Project): List<FileCollection> {
        val dump = project.tasks.getByName(ConfigurationSchemaTaskNames.DUMP) as ConfigSchemaDump
        val collected = dump.runtimeClasspath.from.filterIsInstance<ConfigurableFileCollection>()
        val providers = collected.single().from.filterIsInstance<Provider<*>>()
        val values = providers.map { provider -> provider.get() }
        return values.filterIsInstance<FileCollection>()
    }

    context("configuration schema wiring") {
        test("registers nothing while the feature is disabled") {
            val project = projectWith("java")
            (project as ProjectInternal).evaluate()

            project.tasks.findByName(ConfigurationSchemaTaskNames.DUMP) shouldBe null
            project.tasks.findByName(ConfigurationSchemaTaskNames.CHECK) shouldBe null
            project.tasks.findByName(ConfigurationSchemaTaskNames.VALIDATE) shouldBe null
        }

        test("hands every declared schema to the dump and the check") {
            val project = evaluated(projectWith("java")) { schema ->
                schema.schema("server") { holder.set(HOLDER) }
                schema.schema("gateway") {
                    holder.set(HOLDER)
                    accessor.set("gatewaySchema")
                }
            }
            val dump = project.tasks.getByName(ConfigurationSchemaTaskNames.DUMP) as ConfigSchemaDump
            val check = project.tasks.getByName(ConfigurationSchemaTaskNames.CHECK) as ConfigSchemaCheck

            val declared = dump.schemas.get().map { schema -> schema.toString() }
            declared shouldBe listOf("server -> $HOLDER.server", "gateway -> $HOLDER.gatewaySchema")
            check.schemas.get().size shouldBe 2
            check.dumpTaskPath.get() shouldBe ":${ConfigurationSchemaTaskNames.DUMP}"
            check.schemaDirectory.get().asFile shouldBe project.projectDir.resolve("config-schema")
        }

        test("records the check outcome below the build directory") {
            val project = evaluated(projectWith("java")) { schema ->
                schema.schema("server") { holder.set(HOLDER) }
            }
            val check = project.tasks.getByName(ConfigurationSchemaTaskNames.CHECK) as ConfigSchemaCheck
            val outcome = check.outcomeFile.get()

            outcome.asFile.invariantSeparatorsPath shouldEndWith "build/kreate/configuration/schema-check.txt"
        }

        test("runs the schema check during check") {
            val project = evaluated(projectWith("java")) { schema ->
                schema.schema("server") { holder.set(HOLDER) }
            }

            checkDependenciesOf(project) shouldContain ConfigurationSchemaTaskNames.CHECK
        }

        test("skips the dump and the check when no schema is declared") {
            val project = evaluated(projectWith("java")) { }
            val dump = project.tasks.getByName(ConfigurationSchemaTaskNames.DUMP) as TaskInternal
            val check = project.tasks.getByName(ConfigurationSchemaTaskNames.CHECK) as TaskInternal

            dump.onlyIf.isSatisfiedBy(dump) shouldBe false
            check.onlyIf.isSatisfiedBy(check) shouldBe false
        }

        test("runs the dump and the check once a schema is declared") {
            val project = evaluated(projectWith("java")) { schema ->
                schema.schema("server") { holder.set(HOLDER) }
            }
            val dump = project.tasks.getByName(ConfigurationSchemaTaskNames.DUMP) as TaskInternal
            val check = project.tasks.getByName(ConfigurationSchemaTaskNames.CHECK) as TaskInternal

            dump.onlyIf.isSatisfiedBy(dump) shouldBe true
            check.onlyIf.isSatisfiedBy(check) shouldBe true
        }

        test("registers no validation unless one is declared") {
            val project = evaluated(projectWith("java")) { }

            project.tasks.findByName(ConfigurationSchemaTaskNames.VALIDATE) shouldBe null
            checkDependenciesOf(project) shouldNotContain ConfigurationSchemaTaskNames.VALIDATE
        }

        test("validates the repository's own configuration during check") {
            val project = evaluated(projectWith("java")) { schema ->
                schema.validation {
                    holder.set(HOLDER)
                    accessor.set("reports")
                }
            }
            val validate = project.tasks.getByName(ConfigurationSchemaTaskNames.VALIDATE) as ConfigValidate
            val report = validate.reportFile.get()

            validate.reports.get().toString() shouldBe "validation -> $HOLDER.reports"
            report.asFile.invariantSeparatorsPath shouldEndWith "build/kreate/configuration/validation.txt"
            checkDependenciesOf(project) shouldContain ConfigurationSchemaTaskNames.VALIDATE
        }

        test("reads the declarations from a multiplatform project's JVM target") {
            val project = projectWith("org.jetbrains.kotlin.multiplatform")
            val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
            kotlin.jvm()
            evaluated(project) { schema ->
                schema.schema("server") { holder.set(HOLDER) }
            }

            classpathSourcesOf(project) shouldHaveSize 2
        }

        test("reads the declarations from a JVM project's main runtime classpath") {
            val project = evaluated(projectWith("java")) { schema ->
                schema.schema("server") { holder.set(HOLDER) }
            }

            classpathSourcesOf(project) shouldHaveSize 1
        }
    }
})
