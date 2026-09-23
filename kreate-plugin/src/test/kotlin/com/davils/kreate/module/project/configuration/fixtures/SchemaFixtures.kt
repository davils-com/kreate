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

package com.davils.kreate.module.project.configuration.fixtures

/**
 * A declaration reached the way a top level `val` is reached.
 *
 * `SchemaReflection` accepts both spellings of a Kotlin property, so this file holds one of each: a
 * `val`, whose getter is `getServerSchema`, and a function named without a `get`.
 */
val serverSchema: FakeSchema = FakeSchema("server")

/**
 * A declaration reached the way a function is reached.
 */
fun gatewaySchema(): FakeSchema = FakeSchema("gateway")

/**
 * A declaration reached through the instance a Kotlin `object` holds itself in.
 */
object SchemaHolder {
    val clientSchema: FakeSchema = FakeSchema("client")
}

/**
 * Stands in for a configuration schema, carrying only what the reflection reads back out.
 */
class FakeSchema(val id: String)

/**
 * What a dry run reported, shaped like the report the configuration library hands back.
 */
class FakeReport(private val location: String, private val loadable: Boolean, private val problems: List<String>) {
    fun isLoadable(): Boolean = loadable

    fun getPath(): String = location

    fun getErrors(): List<String> = problems
}

/**
 * The reports a project would expose for the dry run over its own files.
 */
val configurationReports: List<FakeReport> = listOf(
    FakeReport("config://server.yaml", loadable = true, problems = emptyList()),
    FakeReport("config://gateway.yaml", loadable = false, problems = listOf("port: is required"))
)
