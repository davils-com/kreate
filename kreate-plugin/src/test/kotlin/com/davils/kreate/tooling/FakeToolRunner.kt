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

internal class FakeToolRunner(
    private val respond: (ToolInvocation) -> ToolResult
) : ToolRunner {
    private val recorded: MutableList<ToolInvocation> = mutableListOf()

    val invocations: List<ToolInvocation>
        get() = recorded

    override fun run(invocation: ToolInvocation): ToolResult {
        recorded.add(invocation)
        return respond(invocation)
    }

    companion object {
        fun exiting(exitCode: Int, stdout: String = "", stderr: String = ""): FakeToolRunner =
            FakeToolRunner { result(ToolStatus.EXITED, exitCode, stdout, stderr) }

        fun timingOut(): FakeToolRunner = FakeToolRunner { result(ToolStatus.TIMED_OUT, null, "", "") }

        fun notStarting(): FakeToolRunner = FakeToolRunner { result(ToolStatus.NOT_STARTED, null, "", "") }

        private fun result(status: ToolStatus, exitCode: Int?, stdout: String, stderr: String): ToolResult =
            ToolResult(status, exitCode, outputOf(stdout), outputOf(stderr))

        private fun outputOf(text: String): ToolOutput = ToolOutput(text.toByteArray(), isTruncated = false)
    }
}
