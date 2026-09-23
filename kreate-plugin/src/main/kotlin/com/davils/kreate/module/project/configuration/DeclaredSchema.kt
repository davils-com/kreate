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

import com.davils.kreate.module.project.configuration.extension.ConfigurationDefinition
import java.io.Serializable

private const val EXPORT_SUFFIX: String = ".json"

/**
 * One declaration's address, resolved and flattened for a task input.
 *
 * The extension type carries Gradle `Property` objects, and a task that took those as an `@Input`
 * would be carrying the project model into its own execution - which is the thing that stops a task
 * working with the configuration cache. This is the same three strings with nothing live in them.
 *
 * @property name What the declaration is called in the build, and the name its export is written under.
 * @property holder The fully qualified JVM class the declaration is reached through.
 * @property accessor The no-argument method on [holder] that hands the declaration back.
 * @since 3.1.0
 */
public class DeclaredSchema internal constructor(
    public val name: String,
    public val holder: String,
    public val accessor: String
) : Serializable {
    override fun toString(): String = "$name -> $holder.$accessor"

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * The file one declaration's export is written to.
 */
internal fun exportFileName(name: String): String = "$name$EXPORT_SUFFIX"

/**
 * Flattens a declared definition, filling the accessor in from the name when nobody said.
 */
internal fun ConfigurationDefinition.resolved(): DeclaredSchema {
    val declaredName = name.get()

    return DeclaredSchema(
        name = declaredName,
        holder = holder.get(),
        accessor = accessor.getOrElse(declaredName)
    )
}
