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
package com.davils.kreate.standard.provider

import com.davils.kore.core.outcome.Outcome
import com.davils.kreate.net.HttpTransport
import com.davils.kreate.net.HttpTransports
import com.davils.kreate.net.LoopbackServer
import com.davils.kreate.net.NetworkSettings
import com.davils.kreate.net.RetryPolicy
import com.davils.kreate.net.Sleeper
import com.davils.kreate.standard.fetch.RevalidatingCache
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardSnapshot
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import java.time.Clock
import java.time.Duration

internal class ProviderFixture(directory: File, token: String? = null, project: File? = null) : AutoCloseable {
    val server: LoopbackServer = LoopbackServer.http()

    private val transport: HttpTransport =
        HttpTransports.create(NetworkSettings("test"), Clock.systemUTC(), Sleeper { }, RetryPolicy(maxAttempts = 1))

    val context: ProviderContext = contextOf(directory, token, project)

    fun json(path: String, body: String) {
        server.respond(path, LoopbackServer.HTTP_OK, mapOf("Content-Type" to "application/json"), body.toByteArray())
    }

    fun text(path: String, body: String) {
        server.respond(path, LoopbackServer.HTTP_OK, body = body.toByteArray())
    }

    override fun close() {
        transport.close()
        server.close()
    }

    fun bytes(path: String, body: ByteArray) {
        server.respond(path, LoopbackServer.HTTP_OK, body = body)
    }

    private fun contextOf(directory: File, token: String?, project: File?): ProviderContext {
        val base = server.uri("").toString()
        val urls = ServiceEndpoints.DEFAULTS.keys.associateWith { base }
        val cache = RevalidatingCache(directory.toPath(), Duration.ofHours(1), Clock.systemUTC())
        return ProviderContext(ServiceEndpoints(urls), cache, { transport }, { transport }, token, project?.toPath())
    }
}

internal fun StandardProvider.snapshotFor(declaration: StandardDeclaration): StandardSnapshot {
    val outcome = snapshotOf(declaration)
    return outcome.snapshot()
}

internal fun Outcome<StandardSnapshot, LookupFailure>.snapshot(): StandardSnapshot {
    val success = shouldBeInstanceOf<Outcome.Success<StandardSnapshot>>()
    return success.value
}

internal fun Outcome<StandardSnapshot, LookupFailure>.failure(): LookupFailure {
    val failure = shouldBeInstanceOf<Outcome.Failure<LookupFailure>>()
    return failure.failure
}
