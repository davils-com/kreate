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
package com.davils.kreate.detekt.wiring

import com.davils.kreate.detekt.DetektExtension
import com.davils.kreate.detekt.DetektTaskNames
import com.davils.kreate.task.KreateTaskGroup
import dev.detekt.gradle.Detekt
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import dev.detekt.gradle.extensions.DetektExtension as KDetektExtension

private const val DETEKT_PLUGINS_CONFIGURATION: String = "detektPlugins"

private const val KTLINT_WRAPPER: String = "dev.detekt:detekt-rules-ktlint-wrapper"

internal fun Project.addFormatting(extension: DetektExtension) {
    if (!extension.formatting.get()) return
    val toolVersion = extensions.getByType<KDetektExtension>().toolVersion
    dependencies.addProvider(DETEKT_PLUGINS_CONFIGURATION, toolVersion.map { version -> "$KTLINT_WRAPPER:$version" })

    val sources = layout.projectDirectory.dir("src")
    tasks.register<Detekt>(DetektTaskNames.FORMAT_APPLY) {
        group = KreateTaskGroup.DETEKT.label
        description = "Rewrites the Kotlin sources to follow the formatting rules."
        setSource(sources)
        include("**/*.kt", "**/*.kts")
        autoCorrect.set(true)
        ignoreFailures.set(true)
    }
}
