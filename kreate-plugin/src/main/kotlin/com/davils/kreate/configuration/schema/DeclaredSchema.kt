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

import com.davils.kreate.configuration.ConfigurationDefinition
import java.io.Serializable

private const val EXPORT_SUFFIX: String = ".json"

internal class DeclaredSchema(
    val name: String,
    val holder: String,
    val accessor: String
) : Serializable {
    override fun toString(): String = "$name -> $holder.$accessor"

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal fun exportFileName(name: String): String = "$name$EXPORT_SUFFIX"

internal fun ConfigurationDefinition.resolved(): DeclaredSchema {
    val declaredName = name.get()

    return DeclaredSchema(
        name = declaredName,
        holder = holder.get(),
        accessor = accessor.getOrElse(declaredName)
    )
}
