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

package com.davils.kreate.module.project.configuration

import org.gradle.api.GradleException
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.URLClassLoader

/**
 * The facade the configuration library compiles `ConfigSchema.toJsonSchema` into.
 *
 * Named here rather than discovered, because there is nothing to discover it by: a Kotlin top level
 * function is a static method on a class named after its file, and no annotation marks it. Kreate
 * does not depend on the library, so a rename there is invisible to the compiler here - which is why
 * a missing facade is answered by naming it and saying what it belongs to, rather than by a
 * `ClassNotFoundException` out of the middle of a task.
 */
private const val EXPORT_FACADE: String = "com.davils.sira.configuration.export.JsonSchemaExportKt"

/**
 * The facade the configuration library compiles `ConfigSchema.compatibilityWith` into.
 */
private const val COMPATIBILITY_FACADE: String = "com.davils.sira.configuration.schema.SchemaCompatibilityKt"

private const val EXPORT_METHOD: String = "toJsonSchema"
private const val COMPATIBILITY_METHOD: String = "compatibilityWith"
private const val KIND_ACCESSOR: String = "getKind"
private const val LOADABLE_ACCESSOR: String = "isLoadable"
private const val ERRORS_ACCESSOR: String = "getErrors"
private const val PATH_ACCESSOR: String = "getPath"
private const val OBJECT_INSTANCE_FIELD: String = "INSTANCE"
private const val EXPORT_ARGUMENTS: Int = 2
private const val COMPATIBILITY_ARGUMENTS: Int = 2

/**
 * The change kind that means a document already written will simply stop loading.
 */
internal const val BREAKING_KIND: String = "BREAKING"

/**
 * One difference between a schema and its checked-in export, flattened to text.
 *
 * Flattened because the real type lives on the project's classpath and not on this plugin's: a task
 * that handed the object itself back would be handing back something it cannot name.
 */
internal class SchemaDifference(val kind: String, val description: String)

/**
 * One report of what loading a configuration file would do, flattened for the same reason.
 */
internal class ValidationOutcome(val path: String, val isLoadable: Boolean, val problems: List<String>)

/**
 * Reads a project's own declarations out of its compiled classes.
 *
 * **A class loader of its own over the project's runtime classpath, parented to the platform loader.**
 * Parenting to Gradle's would let whatever Gradle happens to carry answer for a class the project
 * declares, which is how a build quietly starts depending on the version of a library it never named.
 */
internal class SchemaReflection(classpath: Collection<File>) : AutoCloseable {
    private val loader: URLClassLoader = URLClassLoader(
        classpath.map { entry -> entry.toURI().toURL() }.toTypedArray(),
        ClassLoader.getPlatformClassLoader()
    )

    /**
     * The declaration [accessor] on [holder] hands back.
     *
     * Both spellings of a Kotlin property are accepted - `getServerSchema` and `serverSchema` - and a
     * member of an `object` is reached through its `INSTANCE` field, so a build file says *where* the
     * declaration is and never how it was compiled.
     */
    fun declarationOf(holder: String, accessor: String): Any {
        val owner = loadClass(holder)
        val getterName = "get${accessor.replaceFirstChar { first -> first.uppercase() }}"
        val method = owner.methods.firstOrNull { candidate ->
            candidate.parameterCount == 0 && (candidate.name == accessor || candidate.name == getterName)
        } ?: throw GradleException(
            "'$holder' has no no-argument '$accessor' or '$getterName'. " +
                "Name the accessor the declaration is reached through."
        )

        val receiver = if (Modifier.isStatic(method.modifiers)) null else instanceOf(owner)

        return method.invoke(receiver)
            ?: throw GradleException("'$holder.$accessor' answered with nothing to export.")
    }

    /**
     * The JSON Schema export of a declaration.
     */
    fun exportOf(schema: Any): String {
        val method = facadeMethod(EXPORT_FACADE, EXPORT_METHOD, EXPORT_ARGUMENTS)
        val exported = method.invoke(null, schema, null)

        return exported as? String ?: throw GradleException(
            "'$EXPORT_FACADE.$EXPORT_METHOD' answered with something that is not an export."
        )
    }

    /**
     * How a declaration differs from the export checked in beside it.
     */
    fun differencesBetween(schema: Any, previousExport: String): List<SchemaDifference> {
        val method = facadeMethod(COMPATIBILITY_FACADE, COMPATIBILITY_METHOD, COMPATIBILITY_ARGUMENTS)
        val changes = method.invoke(null, schema, previousExport) as? List<*> ?: throw GradleException(
            "'$COMPATIBILITY_FACADE.$COMPATIBILITY_METHOD' answered with no list of changes."
        )

        return changes.filterNotNull().map { change -> differenceOf(change) }
    }

    /**
     * What a dry run over the repository's own configuration files reported.
     */
    fun outcomesOf(reports: Any): List<ValidationOutcome> {
        val listed = reports as? List<*> ?: throw GradleException(
            "A validation accessor hands back a list of reports, and this one did not."
        )

        return listed.filterNotNull().map { report -> outcomeOf(report) }
    }

    override fun close() {
        loader.close()
    }

    private fun differenceOf(change: Any): SchemaDifference {
        val kind = change.javaClass.getMethod(KIND_ACCESSOR).invoke(change)

        return SchemaDifference(
            kind = (kind as? Enum<*>)?.name ?: kind.toString(),
            description = change.toString()
        )
    }

    private fun outcomeOf(report: Any): ValidationOutcome {
        val type = report.javaClass
        val isLoadable = type.getMethod(LOADABLE_ACCESSOR).invoke(report) as Boolean
        val path = type.getMethod(PATH_ACCESSOR).invoke(report).toString()
        val errors = type.getMethod(ERRORS_ACCESSOR).invoke(report) as? List<*> ?: emptyList<Any>()

        return ValidationOutcome(path, isLoadable, errors.map { problem -> problem.toString() })
    }

    private fun loadClass(name: String): Class<*> = runCatching { loader.loadClass(name) }
        .getOrElse {
            throw GradleException(
                "'$name' is not on this project's runtime classpath. " +
                    "A schema check needs the configuration library the declaration was written against."
            )
        }

    /**
     * The instance an `object` holds itself in, for a declaration that is a member rather than a top
     * level value.
     */
    private fun instanceOf(owner: Class<*>): Any = runCatching { owner.getField(OBJECT_INSTANCE_FIELD).get(null) }
        .getOrElse {
            throw GradleException(
                "'${owner.name}' has no instance to read the declaration from. " +
                    "Name a top level value or a Kotlin object."
            )
        }

    /**
     * The real method behind a Kotlin extension function, rather than its default-argument bridge.
     *
     * The export takes a nullable title, so the compiler emits `toJsonSchema` and a synthetic
     * `toJsonSchema$default` beside it that takes two more arguments. Matching on the argument count
     * is what picks the first; matching on the name alone would work today and stop working the day a
     * second default is added.
     */
    private fun facadeMethod(facade: String, name: String, arguments: Int): Method {
        val owner = loadClass(facade)

        return owner.methods.firstOrNull { candidate ->
            candidate.name == name && candidate.parameterCount == arguments
        } ?: throw GradleException(
            "'$facade' carries no '$name' taking $arguments arguments. " +
                "This project's configuration library is not one this check knows how to read."
        )
    }
}
