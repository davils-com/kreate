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

package com.davils.kreate.tooling

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File

private const val SUCCESS_EXIT_CODE: Int = 0

internal fun runExternalTool(
    exec: ExecOperations,
    logger: Logger,
    description: String,
    workingDirectory: File,
    arguments: List<String>,
    environment: Map<String, String> = emptyMap()
): String {
    val output = ByteArrayOutputStream()
    val result = exec.exec {
        workingDir = workingDirectory
        commandLine(arguments)
        environment.forEach { (key, value) -> environment(key, value) }
        standardOutput = output
        errorOutput = output
        isIgnoreExitValue = true
    }

    val text = output.toString(Charsets.UTF_8)
    val hasFailed = result.exitValue != SUCCESS_EXIT_CODE

    if (hasFailed) {
        throw GradleException(
            buildString {
                appendLine("$description failed with exit code ${result.exitValue}.")
                appendLine()
                appendLine("Command:           ${arguments.joinToString(" ")}")
                appendLine("Working directory: ${workingDirectory.absolutePath}")
                environment.forEach { (key, value) -> appendLine("$key: $value") }
                appendLine()
                appendLine("Output:")
                append(text.trimEnd())
            }
        )
    }

    logger.info(text)
    return text
}
