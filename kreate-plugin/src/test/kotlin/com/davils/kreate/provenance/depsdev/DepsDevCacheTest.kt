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

import com.davils.kreate.freshDirectory
import com.davils.kreate.net.FIXED_INSTANT
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Duration
import java.time.ZoneOffset

private fun clockAt(hours: Long): Clock = Clock.fixed(FIXED_INSTANT.plus(Duration.ofHours(hours)), ZoneOffset.UTC)

class DepsDevCacheTest : FunSpec({
    val workspace = tempdir()

    context("the cache") {
        test("returns what it holds within its time to live") {
            val directory = workspace.freshDirectory().toPath()
            DepsDevCache(directory, Duration.ofHours(24), clockAt(0)).write("/a", "answer")

            DepsDevCache(directory, Duration.ofHours(24), clockAt(23)).read("/a") shouldBe "answer"
        }

        test("forgets what is older") {
            val directory = workspace.freshDirectory().toPath()
            DepsDevCache(directory, Duration.ofHours(24), clockAt(0)).write("/a", "answer")

            val later = DepsDevCache(directory, Duration.ofHours(24), clockAt(25))

            later.read("/a").shouldBeNull()
        }

        test("keeps keys apart") {
            val cache = DepsDevCache(workspace.freshDirectory().toPath(), Duration.ofHours(1), clockAt(0))
            cache.write("/a", "first")

            cache.read("/b").shouldBeNull()
        }
    }
})
