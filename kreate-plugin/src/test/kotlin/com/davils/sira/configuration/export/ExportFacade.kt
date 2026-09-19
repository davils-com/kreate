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

@file:JvmName("JsonSchemaExportKt")

package com.davils.sira.configuration.export

import com.davils.kreate.module.project.configuration.fixtures.FakeSchema

/**
 * A stand-in for the facade the configuration library compiles its JSON Schema export into.
 *
 * Kreate reaches that facade by name over the project's runtime classpath, so what a test can hold is
 * the *shape* of the contract - the class name, the method name and the argument list - and not that
 * the real library still matches it. The real one is held by the configuration library's own suite
 * and by the first build that runs the check; this is what stops the reflection here from breaking
 * silently while nobody is looking.
 */
fun toJsonSchema(schema: FakeSchema, title: String?): String {
    val named = title ?: schema.id

    return """{"${'$'}version":{"const":1},"title":"$named"}"""
}
