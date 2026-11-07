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
package com.davils.kreate.provenance.depsdev

import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.freshDirectory
import com.davils.kreate.net.FIXED_CLOCK
import com.davils.kreate.net.NetworkException
import com.davils.kreate.net.ScriptedTransport
import com.davils.kreate.net.responseOf
import com.davils.kreate.provenance.PACKAGE_PATH
import com.davils.kreate.provenance.PROJECT_PATH
import com.davils.kreate.provenance.VERSION_PATH
import com.davils.kreate.provenance.historyJson
import com.davils.kreate.provenance.projectJson
import com.davils.kreate.provenance.routedTransport
import com.davils.kreate.provenance.versionJson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import java.time.Instant

private val MODULE: ModuleCoordinate = ModuleCoordinate("org.example", "lib", "1.0.0")

private const val BASE: String = "https://api.deps.dev"

class DepsDevClientTest : FunSpec({
    val workspace = tempdir()

    fun cache(): DepsDevCache = DepsDevCache(workspace.freshDirectory().toPath(), Duration.ofHours(1), FIXED_CLOCK)

    context("deps.dev") {
        test("is asked for the package with an encoded name") {
            val transport = routedTransport(mapOf(PACKAGE_PATH to historyJson("1.0.0" to "2020-01-01T00:00:00Z")))

            val history = DepsDevClient(transport, BASE, cache()).historyOf(MODULE)

            history?.publishedAt("1.0.0") shouldBe Instant.parse("2020-01-01T00:00:00Z")
            transport.requests.single().uri.rawPath shouldBe PACKAGE_PATH
        }

        test("answers nothing for a module it does not know") {
            val client = DepsDevClient(routedTransport(emptyMap()), BASE, cache())

            client.historyOf(MODULE).shouldBeNull()
        }

        test("reads the source repository and its scorecard") {
            val routes = mapOf(VERSION_PATH to versionJson("github.com/example/lib"), PROJECT_PATH to projectJson(7.5))
            val client = DepsDevClient(routedTransport(routes), BASE, cache())

            client.sourceProjectsOf(MODULE) shouldBe listOf("github.com/example/lib")
            client.scorecardOf("github.com/example/lib") shouldBe 7.5
        }

        test("knows no project without a source repository") {
            val tracker = """{"relatedProjects":[{"projectKey":{"id":"x"},"relationType":"ISSUE_TRACKER"}]}"""
            val client = DepsDevClient(routedTransport(mapOf(VERSION_PATH to tracker)), BASE, cache())

            client.sourceProjectsOf(MODULE).shouldBeEmpty()
        }

        test("answers from the cache within its time to live") {
            val transport = routedTransport(mapOf(PACKAGE_PATH to historyJson("1.0.0" to "2020-01-01T00:00:00Z")))
            val client = DepsDevClient(transport, BASE, cache())

            client.historyOf(MODULE)
            client.historyOf(MODULE)

            transport.requests.size shouldBe 1
        }

        test("asks again when the cached history lacks the version") {
            val shared = cache()
            val old = routedTransport(mapOf(PACKAGE_PATH to historyJson("0.9.0" to "2020-01-01T00:00:00Z")))
            DepsDevClient(old, BASE, shared).historyOf(MODULE.copy(version = "0.9.0"))
            val fresh = routedTransport(mapOf(PACKAGE_PATH to historyJson("1.0.0" to "2026-11-05T00:00:00Z")))

            val history = DepsDevClient(fresh, BASE, shared).historyOf(MODULE)

            history?.knows("1.0.0") shouldBe true
            fresh.requests.size shouldBe 1
        }

        test("is unavailable when it answers with an error") {
            val transport = ScriptedTransport { request -> responseOf(503, request.uri.toString()) }

            val failure = shouldThrow<DepsDevUnavailableException> {
                DepsDevClient(transport, BASE, cache()).historyOf(MODULE)
            }

            failure.message shouldContain "503"
        }

        test("is unavailable when it cannot be reached") {
            val transport = ScriptedTransport { throw NetworkException.Unavailable("down") }

            shouldThrow<DepsDevUnavailableException> { DepsDevClient(transport, BASE, cache()).historyOf(MODULE) }
        }

        test("is unavailable when it answers with something other than JSON") {
            val transport = routedTransport(mapOf(PACKAGE_PATH to "<html>"))

            shouldThrow<DepsDevUnavailableException> { DepsDevClient(transport, BASE, cache()).historyOf(MODULE) }
        }
    }
})
