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

package com.davils.kreate.jni.task

import com.davils.kreate.tooling.runExternalTool
import org.gradle.api.logging.Logger
import org.gradle.process.ExecOperations
import java.io.File

internal fun runCmake(
    exec: ExecOperations,
    logger: Logger,
    phase: String,
    workingDirectory: File,
    javaHome: String,
    arguments: List<String>
) {
    runExternalTool(
        exec = exec,
        logger = logger,
        description = "CMake $phase",
        workingDirectory = workingDirectory,
        arguments = arguments,
        environment = mapOf("JAVA_HOME" to javaHome)
    )
}
