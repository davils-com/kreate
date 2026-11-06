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

import com.davils.kreate.capturedLogOf
import com.davils.kreate.freshDirectory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder

class TrivyLockFileScanTest : FunSpec({
    val workspace = tempdir()

    fun scanOf(type: Class<out TrivyLockFileScan>, failOnMissingLockFiles: Boolean): TrivyLockFileScan {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        val task = project.tasks.register("scan", type).get()
        task.failOnMissingLockFiles.set(failOnMissingLockFiles)
        task.severity.set(emptyList())
        task.failOnFindings.set(true)
        task.gated.set(false)
        task.tool.version.set("0.0.0")
        task.tool.useInstalled.set(true)
        task.tool.checksums.set(emptyMap())
        task.tool.downloadBaseUrl.set("https://example.invalid")
        task.tool.offline.set(true)
        task.documentFile.set(project.layout.buildDirectory.file("document.json"))
        return task
    }

    fun skippedLogOf(type: Class<out TrivyLockFileScan>): String {
        val task = scanOf(type, failOnMissingLockFiles = false)
        return capturedLogOf { task.execute() }
    }

    context("TrivyLockFileScan without lock files") {
        test("the vulnerability scan fails rather than report nothing as clean") {
            val task = scanOf(TrivyVulnerabilityScan::class.java, failOnMissingLockFiles = true)

            val failure = shouldThrow<GradleException> { task.execute() }

            failure.message.orEmpty() shouldContain "vulnerability scan cannot check anything"
            failure.message.orEmpty() shouldContain "kreateResolveAndLockAll --write-locks"
        }

        test("the license scan fails rather than report nothing as clean") {
            val task = scanOf(TrivyLicenseScan::class.java, failOnMissingLockFiles = true)

            val failure = shouldThrow<GradleException> { task.execute() }

            failure.message.orEmpty() shouldContain "license scan cannot check anything"
        }

        test("the vulnerability scan names itself when a project opted out") {
            val log = skippedLogOf(TrivyVulnerabilityScan::class.java)

            log shouldContain "Skipping vulnerability scan."
            log shouldNotContain "license"
        }

        test("the license scan names itself when a project opted out") {
            val log = skippedLogOf(TrivyLicenseScan::class.java)

            log shouldContain "Skipping license scan."
        }
    }
})
