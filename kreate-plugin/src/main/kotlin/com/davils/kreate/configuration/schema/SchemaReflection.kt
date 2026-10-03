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

package com.davils.kreate.configuration.schema

import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.URLClassLoader

private const val EXPORT_FACADE: String = "com.davils.sira.configuration.export.JsonSchemaExportKt"

private const val COMPATIBILITY_FACADE: String = "com.davils.sira.configuration.schema.SchemaCompatibilityKt"

private const val EXPORT_METHOD: String = "toJsonSchema"
private const val COMPATIBILITY_METHOD: String = "compatibilityWith"
private const val KIND_ACCESSOR: String = "getKind"
private const val LOADABLE_ACCESSOR: String = "isLoadable"
private const val ERRORS_ACCESSOR: String = "getErrors"
private const val PATH_ACCESSOR: String = "getPath"
private const val GETTER_PREFIX: String = "get"
private const val OBJECT_INSTANCE_FIELD: String = "INSTANCE"
private const val EXPORT_ARGUMENTS: Int = 2
private const val COMPATIBILITY_ARGUMENTS: Int = 2

internal class SchemaReflection(classpath: Collection<File>) : AutoCloseable {
    private val loader: URLClassLoader = URLClassLoader(
        classpath.map { entry -> entry.toURI().toURL() }.toTypedArray(),
        ClassLoader.getPlatformClassLoader()
    )

    fun declarationOf(holder: String, accessor: String): Any {
        val owner = loadClass(holder)
        val getterName = "$GETTER_PREFIX${accessor.replaceFirstChar { first -> first.uppercase() }}"
        val method = owner.methods.firstOrNull { candidate -> isAccessor(candidate, setOf(accessor, getterName)) }
        requireNotNull(method) {
            "'$holder' has no no-argument '$accessor' or '$getterName'. " +
                "Name the accessor the declaration is reached through."
        }

        val declaration = invokeReporting(method, receiverOf(method, owner))
        return checkNotNull(declaration) { "'$holder.$accessor' answered with nothing to export." }
    }

    fun exportOf(schema: Any): String {
        val method = facadeMethod(EXPORT_FACADE, EXPORT_METHOD, EXPORT_ARGUMENTS)
        val exported = invokeReporting(method, null, schema, null)

        return exported as? String ?: error(
            "'$EXPORT_FACADE.$EXPORT_METHOD' answered with something that is not an export."
        )
    }

    fun differencesBetween(schema: Any, previousExport: String): List<SchemaDifference> {
        val method = facadeMethod(COMPATIBILITY_FACADE, COMPATIBILITY_METHOD, COMPATIBILITY_ARGUMENTS)
        val changes = invokeReporting(method, null, schema, previousExport) as? List<*> ?: error(
            "'$COMPATIBILITY_FACADE.$COMPATIBILITY_METHOD' answered with no list of changes."
        )

        return changes.filterNotNull().map { change -> differenceOf(change) }
    }

    fun outcomesOf(reports: Any): List<ValidationOutcome> {
        val listed = reports as? List<*>
        requireNotNull(listed) { "A validation accessor hands back a list of reports, and this one did not." }

        return listed.filterNotNull().map { report -> outcomeOf(report) }
    }

    override fun close() {
        loader.close()
    }

    private fun isAccessor(candidate: Method, acceptedNames: Set<String>): Boolean =
        candidate.parameterCount == 0 && candidate.name in acceptedNames

    private fun receiverOf(method: Method, owner: Class<*>): Any? {
        if (Modifier.isStatic(method.modifiers)) return null
        return instanceOf(owner)
    }

    private fun differenceOf(change: Any): SchemaDifference {
        val kind = invokeAccessor(change, KIND_ACCESSOR)

        return SchemaDifference(
            kind = (kind as? Enum<*>)?.name ?: kind.toString(),
            description = change.toString()
        )
    }

    private fun outcomeOf(report: Any): ValidationOutcome {
        val isLoadable = invokeAccessor(report, LOADABLE_ACCESSOR) as? Boolean
        checkNotNull(isLoadable) {
            "'${report.javaClass.name}.$LOADABLE_ACCESSOR' answered with something that is not a Boolean."
        }
        val path = invokeAccessor(report, PATH_ACCESSOR).toString()
        val errors = invokeAccessor(report, ERRORS_ACCESSOR) as? List<*> ?: emptyList<Any>()

        return ValidationOutcome(path, isLoadable, errors.map { problem -> problem.toString() })
    }

    private fun invokeAccessor(target: Any, accessor: String): Any? {
        val method = target.javaClass.getMethod(accessor)
        return invokeReporting(method, target)
    }

    private fun invokeReporting(method: Method, receiver: Any?, vararg arguments: Any?): Any? =
        try {
            method.invoke(receiver, *arguments)
        } catch (thrown: InvocationTargetException) {
            val cause = thrown.cause ?: thrown
            throw IllegalStateException(
                "'${method.declaringClass.name}.${method.name}' threw while Kreate read it: $cause",
                cause
            )
        }

    private fun loadClass(name: String): Class<*> =
        try {
            loader.loadClass(name)
        } catch (missing: ClassNotFoundException) {
            throw IllegalArgumentException(
                "'$name' is not on this project's runtime classpath. " +
                    "A schema check needs the configuration library the declaration was written against.",
                missing
            )
        }

    private fun instanceOf(owner: Class<*>): Any? =
        try {
            owner.getField(OBJECT_INSTANCE_FIELD).get(null)
        } catch (unreachable: ReflectiveOperationException) {
            throw IllegalArgumentException(
                "'${owner.name}' has no instance to read the declaration from. " +
                    "Name a top level value or a Kotlin object.",
                unreachable
            )
        }

    private fun facadeMethod(facade: String, name: String, arguments: Int): Method {
        val owner = loadClass(facade)

        return owner.methods.firstOrNull { candidate ->
            candidate.name == name && candidate.parameterCount == arguments
        } ?: error(
            "'$facade' carries no '$name' taking $arguments arguments. " +
                "This project's configuration library is not one this check knows how to read."
        )
    }
}
