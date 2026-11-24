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

import com.davils.kreate.tooling.ExecOperationsToolRunner
import com.davils.kreate.tooling.ExecutableResolver
import com.davils.kreate.tooling.ExternalTool
import com.davils.kreate.tooling.ToolNotFoundException
import com.davils.kreate.vcs.GitHistory
import com.davils.kreate.vcs.GitUnavailableException
import org.gradle.api.logging.Logger
import org.gradle.process.ExecOperations
import java.io.File

internal fun headRevisionOrNull(exec: ExecOperations, directory: File, logger: Logger): String? =
    try {
        val git = ExecutableResolver.resolve(ExternalTool.GIT)
        GitHistory(ExecOperationsToolRunner(exec), git, directory).head()
    } catch (exception: GitUnavailableException) {
        logger.info("Git could not name the commit of {}.", directory, exception)
        null
    } catch (exception: ToolNotFoundException) {
        logger.info("Git is not installed.", exception)
        null
    }
