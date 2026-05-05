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

package com.davils.kreate.module.project.trivy.tasks

import com.davils.kreate.jobs.Task
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Lifecycle task that runs all enabled Trivy scans.
 *
 * This task serves as an aggregator for [TrivyLicenseScan], [TrivyVulnerabilityScan],
 * and [TrivySecretScan]. Running this task ensures that all security and
 * compliance checks are performed for the project.
 *
 * @since 1.2.0
 */
@DisableCachingByDefault(because = "Lifecycle task that only aggregates other tasks")
public abstract class TrivyScan : Task(
    "Runs all enabled Trivy scans (License, Vulnerability, and Secret).",
    "kreate trivy"
) {
    /**
     * Executes the lifecycle task.
     *
     * This method is a no-op as the task primarily functions through its dependencies.
     *
     * @since 1.2.0
     */
    @TaskAction
    override fun execute() {
        // Lifecycle aggregator
    }
}
