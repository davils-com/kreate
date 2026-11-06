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
package com.davils.kreate.trivy.task

import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import com.davils.kreate.tooling.ExecOperationsToolRunner
import com.davils.kreate.trivy.provision.TrivyUnavailableException
import org.gradle.api.GradleException
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import javax.inject.Inject

@UntrackedTask(because = "It installs into the Gradle user home, which it checks on every run")
internal abstract class TrivyProvision @Inject constructor(
    private val exec: ExecOperations
) : KreateTask("Makes the pinned Trivy available, for example to warm a CI cache.", KreateTaskGroup.TRIVY) {
    @get:Nested
    public abstract val tool: TrivyToolInput

    @TaskAction
    public fun provision() {
        val runner = ExecOperationsToolRunner(exec)
        val command = try {
            tool.provisioner(runner, temporaryDir, logger).provision()
        } catch (cause: TrivyUnavailableException) {
            throw GradleException(cause.message.orEmpty(), cause)
        }
        logger.lifecycle("Trivy: $command")
    }
}
