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

package com.davils.kreate.publish.task

import com.davils.kreate.publish.guard.ReleaseCandidate
import com.davils.kreate.publish.guard.ReleaseGuardRules
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import com.davils.kreate.tooling.ExecOperationsToolRunner
import com.davils.kreate.tooling.ExecutableResolver
import com.davils.kreate.tooling.ExternalTool
import com.davils.kreate.tooling.ToolNotFoundException
import com.davils.kreate.vcs.GitHistory
import com.davils.kreate.vcs.GitUnavailableException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.api.tasks.VerificationException
import org.gradle.process.ExecOperations
import javax.inject.Inject

private const val HEAD: String = "HEAD"

@UntrackedTask(because = "The verdict depends on the environment and the Git history of the build")
internal abstract class ReleaseGuardCheck @Inject constructor(private val exec: ExecOperations) : KreateTask(
    "Refuses to publish a version that does not belong to the build it comes from.",
    KreateTaskGroup.KREATE
) {
    @get:Internal
    public abstract val declaredVersion: Property<String>

    @get:Internal
    public abstract val resolvedVersion: Property<String>

    @get:Internal
    public abstract val tag: Property<String>

    @get:Internal
    public abstract val tagPrefix: Property<String>

    @get:Internal
    public abstract val releaseBranch: Property<String>

    @get:Internal
    public abstract val remote: Property<String>

    @get:Internal
    public abstract val tools: MapProperty<String, String>

    @get:Internal
    public abstract val projectDirectory: DirectoryProperty

    @TaskAction
    public fun verify() {
        val candidate = ReleaseCandidate(
            declaredVersion = declaredVersion.orNull,
            resolvedVersion = resolvedVersion.get(),
            tag = tag.orNull,
            tagPrefix = tagPrefix.get(),
            tools = tools.get()
        )
        val problems = ReleaseGuardRules.problemsOf(candidate) + branchProblems(candidate)
        if (problems.isEmpty()) return logger.lifecycle("Version ${candidate.resolvedVersion} may be published.")
        val lines = problems.joinToString("\n") { problem -> "  $problem" }
        throw VerificationException("The release guard refuses to publish:\n$lines")
    }

    private fun branchProblems(candidate: ReleaseCandidate): List<String> {
        val tagName = candidate.tag ?: return emptyList()
        val branch = "${remote.get()}/${releaseBranch.get()}"
        val isOnBranch = isAncestorOf(branch)
        if (isOnBranch == true) return emptyList()
        if (isOnBranch == false) return listOf("The release tag '$tagName' is on a commit that is not part of $branch.")
        return listOf("Git could not tell whether the release tag '$tagName' is part of $branch.")
    }

    private fun isAncestorOf(branch: String): Boolean? {
        return try {
            val git = ExecutableResolver.resolve(ExternalTool.GIT)
            val history = GitHistory(ExecOperationsToolRunner(exec), git, projectDirectory.get().asFile)
            history.isAncestor(HEAD, branch)
        } catch (exception: GitUnavailableException) {
            logger.info("Git could not answer the release guard.", exception)
            null
        } catch (exception: ToolNotFoundException) {
            logger.info("Git is not installed.", exception)
            null
        } catch (exception: IllegalArgumentException) {
            logger.info("The release branch is no valid revision.", exception)
            null
        }
    }
}
