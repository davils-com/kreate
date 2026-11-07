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
package com.davils.kreate.vcs

import com.davils.kreate.tooling.ToolInvocation
import com.davils.kreate.tooling.ToolResult
import com.davils.kreate.tooling.ToolRunner

internal class RecordingRunner(private val answer: (List<String>) -> ToolResult) : ToolRunner {
    val invocations: MutableList<ToolInvocation> = mutableListOf()

    override fun run(invocation: ToolInvocation): ToolResult {
        invocations += invocation
        return answer(invocation.arguments)
    }
}
