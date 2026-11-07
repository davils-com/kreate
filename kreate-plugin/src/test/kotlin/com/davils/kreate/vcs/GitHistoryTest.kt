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

import com.davils.kreate.tooling.ToolOutput
import com.davils.kreate.tooling.ToolResult
import com.davils.kreate.tooling.ToolRunner
import com.davils.kreate.tooling.ToolStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.io.File

internal fun resultOf(exitCode: Int, stdout: String = ""): ToolResult =
    ToolResult(ToolStatus.EXITED, exitCode, ToolOutput(stdout.toByteArray(), false), ToolOutput(ByteArray(0), false))

private fun historyOf(runner: ToolRunner): GitHistory = GitHistory(runner, "git", File("."))

private fun listingOf(arguments: List<String>): ToolResult {
    if ("diff" in arguments) return resultOf(0, "a/One.kt\nb/Two.kt\n")
    return resultOf(0, "b/Two.kt\nc/New.kt\n")
}

class GitHistoryTest : FunSpec({
    context("the release tags") {
        test("pick the highest version with the prefix") {
            val runner = RecordingRunner { resultOf(0, "v1.2.0\nv1.10.0\nother\nv2.0.0-rc.1\n1.9.0\n") }

            historyOf(runner).latestTag("v") shouldBe ReleaseTag("v2.0.0-rc.1", SemanticVersion(2, 0, 0, "rc.1"))
        }

        test("are none in a repository without tags") {
            val history = historyOf(RecordingRunner { resultOf(0) })

            history.latestTag("").shouldBeNull()
        }
    }

    context("git") {
        test("runs hardened, without a pager and without prompts") {
            val runner = RecordingRunner { resultOf(0, "1.0.0\n") }

            historyOf(runner).latestTag("")

            val invocation = runner.invocations.single()
            invocation.arguments shouldContainAll listOf("core.fsmonitor=false", "--no-pager", "--merged")
            invocation.environment["GIT_TERMINAL_PROMPT"] shouldBe "0"
        }

        test("splits the log into commits with their whole message") {
            val log = "abc\u001ffeat: one\n\nbody\u001e\ndef\u001ffix!: two\u001e\n"

            val commits = historyOf(RecordingRunner { resultOf(0, log) }).commits("1.0.0")

            commits shouldBe listOf(GitCommit("abc", "feat: one\n\nbody"), GitCommit("def", "fix!: two"))
        }

        test("lists changed and untracked files once") {
            val runner = RecordingRunner { arguments -> listingOf(arguments) }

            historyOf(runner).changedFiles("origin/main") shouldBe listOf("a/One.kt", "b/Two.kt", "c/New.kt")
        }

        test("resolves the path of a hook against the project") {
            val runner = RecordingRunner { resultOf(0, ".git/hooks/pre-commit\n") }

            historyOf(runner).gitPath("hooks/pre-commit") shouldBe File(".").resolve(".git/hooks/pre-commit")
        }

        test("refuses a revision that would be read as an option") {
            shouldThrow<IllegalArgumentException> { historyOf(RecordingRunner { resultOf(0) }).show("--output=x", "a") }
        }

        test("answers nothing for a file the revision lacks") {
            val history = historyOf(RecordingRunner { resultOf(128) })

            history.show("1.0.0", "api/a.api").shouldBeNull()
        }
    }
})
