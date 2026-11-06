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

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.gradle.process.ExecOperations
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.util.concurrent.ExecutionException
import kotlin.time.Duration.Companion.seconds

private val WORKING_DIRECTORY: File = Files.createTempDirectory("kreate-tool-runner").toFile()

private fun stubCommand(vararg arguments: String): List<String> {
    val java = File(System.getProperty("java.home"), "bin/java").path
    val locations = listOf(ToolRunnerStub::class.java, Unit::class.java).map { type -> codeLocationOf(type) }
    val classpath = locations.joinToString(File.pathSeparator)
    return listOf(java, "-cp", classpath, ToolRunnerStub::class.java.name) + arguments
}

private fun codeLocationOf(type: Class<*>): String {
    val location = type.protectionDomain.codeSource.location
    return File(location.toURI()).path
}

class ToolRunnerTest : FunSpec({
    val project = ProjectBuilder.builder().build()
    val holder = project.objects.newInstance(ExecOperationsHolder::class.java)
    val runner = ExecOperationsToolRunner(holder.exec)

    test("captures stdout and stderr separately") {
        val invocation = ToolInvocation(stubCommand("print", "to out", "to err", "0"), WORKING_DIRECTORY)

        val result = runner.run(invocation)

        result.status shouldBe ToolStatus.EXITED
        result.exitCode shouldBe 0
        result.stdout.text shouldBe "to out"
        result.stderr.text shouldBe "to err"
    }

    test("returns a failing exit code instead of throwing") {
        val invocation = ToolInvocation(stubCommand("print", "", "broken", "3"), WORKING_DIRECTORY)

        val result = runner.run(invocation)

        result.status shouldBe ToolStatus.EXITED
        result.exitCode shouldBe 3
        result.stderr.text shouldBe "broken"
    }

    test("passes the environment on") {
        val invocation = ToolInvocation(
            stubCommand("print", "value", "", "0"),
            WORKING_DIRECTORY,
            mapOf("KREATE_STUB_SUFFIX" to "-suffix")
        )

        runner.run(invocation).stdout.text shouldBe "value-suffix"
    }

    test("keeps the head of an oversized output and drains the rest") {
        val invocation = ToolInvocation(
            stubCommand("flood", "1000000"),
            WORKING_DIRECTORY,
            maxOutputBytes = 1024
        )

        val result = runner.run(invocation)

        result.exitCode shouldBe 0
        result.stdout.bytes shouldHaveSize 1024
        result.stdout.isTruncated.shouldBeTrue()
    }

    test("marks an output within the limit as complete") {
        val invocation = ToolInvocation(stubCommand("flood", "1024"), WORKING_DIRECTORY, maxOutputBytes = 1024)

        runner.run(invocation).stdout.isTruncated.shouldBeFalse()
    }

    test("stops a hanging tool at its timeout and kills it") {
        val invocation = ToolInvocation(stubCommand("hang"), WORKING_DIRECTORY, timeout = Duration.ofSeconds(5))
        val start = System.nanoTime()

        val result = runner.run(invocation)

        result.status shouldBe ToolStatus.TIMED_OUT
        result.exitCode.shouldBeNull()
        Duration.ofNanos(System.nanoTime() - start).toSeconds() shouldBeLessThan 30L

        val pid = result.stdout.text.trim().toLong()
        eventually(10.seconds) {
            val process = ProcessHandle.of(pid)
            process.filter { handle -> handle.isAlive }.isPresent shouldBe false
        }
    }

    test("gives up on the tool when the waiting thread is interrupted") {
        val invocation = ToolInvocation(stubCommand("hang"), WORKING_DIRECTORY)

        Thread.currentThread().interrupt()

        shouldThrow<InterruptedException> { runner.run(invocation) }
    }

    test("reports a tool that cannot be started") {
        val invocation = ToolInvocation(listOf("kreate-tool-that-does-not-exist"), WORKING_DIRECTORY)

        val result = runner.run(invocation)

        result.status shouldBe ToolStatus.NOT_STARTED
        result.exitCode.shouldBeNull()
    }

    test("lets a failure that is not about the tool escape") {
        val exec = mockk<ExecOperations>()
        every { exec.exec(any()) } throws IllegalStateException("a bug")
        val invocation = ToolInvocation(listOf("tool"), WORKING_DIRECTORY)

        val failure = shouldThrow<ExecutionException> { ExecOperationsToolRunner(exec).run(invocation) }

        failure.cause?.message shouldBe "a bug"
    }

    context("the fake") {
        val invocation = ToolInvocation(listOf("tool"), WORKING_DIRECTORY)

        test("records what it was asked to run") {
            val fake = FakeToolRunner.exiting(2, "out", "err")

            val result = fake.run(invocation)

            fake.invocations shouldBe listOf(invocation)
            result.exitCode shouldBe 2
            result.stdout.text shouldBe "out"
            result.stderr.text shouldBe "err"
        }

        test("can time out") {
            FakeToolRunner.timingOut().run(invocation).status shouldBe ToolStatus.TIMED_OUT
        }

        test("can fail to start") {
            FakeToolRunner.notStarting().run(invocation).status shouldBe ToolStatus.NOT_STARTED
        }
    }

    test("has sensible defaults") {
        val invocation = ToolInvocation(listOf("tool"), WORKING_DIRECTORY)

        invocation.environment shouldBe emptyMap()
        invocation.timeout shouldBe ToolInvocation.DEFAULT_TIMEOUT
    }
})
