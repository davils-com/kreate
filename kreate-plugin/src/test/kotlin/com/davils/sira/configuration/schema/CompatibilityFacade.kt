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

@file:JvmName("SchemaCompatibilityKt")

package com.davils.sira.configuration.schema

import com.davils.kreate.module.project.configuration.fixtures.FakeSchema

private const val BREAKING_MARKER: String = "breaking"

/**
 * What one schema change means for documents already written, shaped like the real enum.
 */
enum class FakeChangeKind { COMPATIBLE, REQUIRES_MIGRATION, BREAKING }

/**
 * One difference, shaped like the real one: a kind read through `getKind`, and prose in `toString`.
 */
class FakeChange(private val path: String, val kind: FakeChangeKind) {
    override fun toString(): String = "$path: changed"
}

/**
 * A stand-in for the facade the configuration library compiles its compatibility check into.
 *
 * The previous export decides the answer, so a test says what it wants by what it checks in: an
 * export carrying the marker is reported as breaking, anything else as compatible. What the real
 * library decides is the real library's business - this holds only that Kreate reads the kind off
 * each change and refuses on the breaking one.
 */
fun compatibilityWith(schema: FakeSchema, previousJsonSchema: String): List<FakeChange> {
    val isBreaking = previousJsonSchema.contains(BREAKING_MARKER)
    val kind = if (isBreaking) FakeChangeKind.BREAKING else FakeChangeKind.COMPATIBLE

    return listOf(FakeChange(schema.id, kind))
}
