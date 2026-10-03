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
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testfixtures.ProjectBuilder

class TrivyLockFileScanTest : FunSpec({
    val workspace = tempdir()

    fun skippedLogOf(type: Class<out TrivyLockFileScan>): String {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        val task = project.tasks.register("scan", type).get()
        return capturedLogOf { task.execute() }
    }

    context("TrivyLockFileScan without lock files") {
        test("the vulnerability scan names itself when it is skipped") {
            val log = skippedLogOf(TrivyVulnerabilityScan::class.java)

            log shouldContain "Skipping vulnerability scan."
            log shouldNotContain "license"
        }

        test("the license scan names itself when it is skipped") {
            val log = skippedLogOf(TrivyLicenseScan::class.java)

            log shouldContain "Skipping license scan."
        }
    }
})
