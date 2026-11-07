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
import com.davils.kreate.tooling.ToolStatus
import java.io.File
import java.time.Duration

private val SAFE_REVISION: Regex = Regex("^[A-Za-z0-9._/+-]+$")

private const val FIELD_SEPARATOR: Char = '\u001f'

private const val RECORD_SEPARATOR: Char = '\u001e'

private val GIT_TIMEOUT: Duration = Duration.ofMinutes(2)

private val HARDENING: List<String> = listOf("-c", "core.fsmonitor=false", "-c", "core.hooksPath=", "--no-pager")

internal class GitHistory(private val runner: ToolRunner, private val git: String, private val directory: File) {
    fun isRepository(): Boolean = run("rev-parse", "--is-inside-work-tree").isSuccess()

    fun root(): File {
        val result = run("rev-parse", "--show-toplevel")
        if (!result.isSuccess()) throw GitUnavailableException("${directory.path} is not inside a Git repository.")
        return File(result.stdout.text.trim())
    }

    fun latestTag(prefix: String): ReleaseTag? {
        val result = run("tag", "--merged", "HEAD", "--list")
        if (!result.isSuccess()) return null
        val tags = result.stdout.text.lines().mapNotNull { line -> ReleaseTag.parse(line.trim(), prefix) }
        return tags.maxByOrNull { tag -> tag.version }
    }

    fun show(revision: String, path: String): String? {
        requireRevision(revision)
        val result = run("show", "$revision:$path")
        if (!result.isSuccess()) return null
        return result.stdout.text
    }

    fun commits(from: String?): List<GitCommit> {
        from?.let(::requireRevision)
        val range = from?.let { start -> "$start..HEAD" } ?: "HEAD"
        val result = run("log", "--no-merges", "--format=%H$FIELD_SEPARATOR%B$RECORD_SEPARATOR", range, "--")
        if (!result.isSuccess()) throw GitUnavailableException("git log failed: ${result.stderr.text.trim()}")
        val records = result.stdout.text.split(RECORD_SEPARATOR).map { record -> record.trim() }
        return records.filter { record -> record.isNotEmpty() }.map(::commitOf)
    }

    private fun commitOf(record: String): GitCommit {
        val hash = record.substringBefore(FIELD_SEPARATOR)
        return GitCommit(hash, record.substringAfter(FIELD_SEPARATOR).trim())
    }

    private fun requireRevision(revision: String) {
        require(SAFE_REVISION.matches(revision) && !revision.startsWith("-")) { "'$revision' is no valid revision." }
    }

    private fun run(vararg arguments: String): ToolResult {
        val invocation = ToolInvocation(
            arguments = listOf(git) + HARDENING + arguments,
            workingDirectory = directory,
            environment = mapOf("GIT_TERMINAL_PROMPT" to "0", "LC_ALL" to "C"),
            timeout = GIT_TIMEOUT
        )
        val result = runner.run(invocation)
        if (result.status != ToolStatus.EXITED) throw GitUnavailableException("git did not finish: ${result.status}.")
        return result
    }

    private fun ToolResult.isSuccess(): Boolean = exitCode == 0
}
