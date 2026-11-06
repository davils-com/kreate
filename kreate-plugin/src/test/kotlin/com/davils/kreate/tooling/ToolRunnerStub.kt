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

object ToolRunnerStub {
    private const val SLEEP_MILLIS: Long = 60_000

    @JvmStatic
    fun main(arguments: Array<String>) {
        val command = arguments.first()
        val parameters = arguments.drop(1)
        when (command) {
            "print" -> print(parameters)
            "flood" -> flood(parameters.single().toInt())
            "hang" -> hang()
        }
    }

    private fun print(parameters: List<String>) {
        val (standardOutput, standardError, exitCode) = parameters
        System.out.print(standardOutput + System.getenv("KREATE_STUB_SUFFIX").orEmpty())
        System.err.print(standardError)
        System.out.flush()
        System.err.flush()
        Runtime.getRuntime().halt(exitCode.toInt())
    }

    private fun flood(bytes: Int) {
        System.out.write(ByteArray(bytes) { 'x'.code.toByte() })
        System.out.flush()
    }

    private fun hang() {
        System.out.println(ProcessHandle.current().pid())
        System.out.flush()
        Thread.sleep(SLEEP_MILLIS)
    }
}
