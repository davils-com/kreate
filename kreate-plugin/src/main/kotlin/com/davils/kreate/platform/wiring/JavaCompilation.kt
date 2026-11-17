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

package com.davils.kreate.platform.wiring

import com.davils.kreate.KreateExtension
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.withType

private val STRICT_JAVAC_ARGUMENTS: List<String> = listOf("-Xlint:all", "-Werror")

internal fun Project.configureJavaCompilation(extension: KreateExtension) {
    val platform = extension.platform
    val isStrict = platform.jvm.javaWarningsAsErrors.getOrElse(false)
    if (!isStrict) return
    val release = platform.javaVersion.get().majorVersion.toInt()

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(release)
        options.compilerArgs.addAll(STRICT_JAVAC_ARGUMENTS)
    }
}
