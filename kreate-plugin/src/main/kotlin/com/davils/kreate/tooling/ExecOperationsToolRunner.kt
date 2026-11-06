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

import org.gradle.api.GradleException
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

private const val WORKER_THREAD_NAME: String = "kreate-tool-runner"

private const val ABORT_GRACE_MILLIS: Long = 5_000

internal class ExecOperationsToolRunner(private val exec: ExecOperations) : ToolRunner {
    override fun run(invocation: ToolInvocation): ToolResult {
        val stdout = BoundedOutputStream(invocation.maxOutputBytes)
        val stderr = BoundedOutputStream(invocation.maxOutputBytes)
        val execution = FutureTask { execute(invocation, stdout, stderr) }
        val worker = Thread(execution, WORKER_THREAD_NAME)
        worker.isDaemon = true
        worker.start()

        val outcome = await(execution, worker, invocation)
        return ToolResult(outcome.status, outcome.exitCode, stdout.snapshot(), stderr.snapshot())
    }

    private fun execute(
        invocation: ToolInvocation,
        stdout: BoundedOutputStream,
        stderr: BoundedOutputStream
    ): ExecResult = exec.exec {
        workingDir = invocation.workingDirectory
        commandLine(invocation.arguments)
        environment(invocation.environment)
        standardOutput = stdout
        errorOutput = stderr
        isIgnoreExitValue = true
    }

    private fun await(execution: FutureTask<ExecResult>, worker: Thread, invocation: ToolInvocation): ExitOutcome =
        try {
            val result = execution.get(invocation.timeout.toMillis(), TimeUnit.MILLISECONDS)
            ExitOutcome(ToolStatus.EXITED, result.exitValue)
        } catch (expected: TimeoutException) {
            execution.cancel(true)
            worker.join(ABORT_GRACE_MILLIS)
            ExitOutcome(ToolStatus.TIMED_OUT, exitCode = null)
        } catch (failure: ExecutionException) {
            val cause = failure.cause
            if (cause !is GradleException) throw failure
            ExitOutcome(ToolStatus.NOT_STARTED, exitCode = null)
        } catch (interrupted: InterruptedException) {
            execution.cancel(true)
            throw interrupted
        }

    private class ExitOutcome(val status: ToolStatus, val exitCode: Int?)
}
