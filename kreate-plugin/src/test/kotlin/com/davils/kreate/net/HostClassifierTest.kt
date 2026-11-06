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

package com.davils.kreate.net

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class HostClassifierTest : FunSpec({
    mapOf(
        "localhost" to HostKind.LOOPBACK,
        "LOCALHOST" to HostKind.LOOPBACK,
        "localhost." to HostKind.LOOPBACK,
        "127.0.0.1" to HostKind.LOOPBACK,
        "127.255.255.254" to HostKind.LOOPBACK,
        "[::1]" to HostKind.LOOPBACK,
        "[0:0:0:0:0:0:0:1]" to HostKind.LOOPBACK,
        "[::ffff:127.0.0.1]" to HostKind.LOOPBACK,
        "0.0.0.0" to HostKind.PRIVATE,
        "[::]" to HostKind.PRIVATE,
        "10.1.2.3" to HostKind.PRIVATE,
        "172.16.0.1" to HostKind.PRIVATE,
        "192.168.1.1" to HostKind.PRIVATE,
        "169.254.169.254" to HostKind.PRIVATE,
        "100.100.100.200" to HostKind.PRIVATE,
        "224.0.0.1" to HostKind.PRIVATE,
        "[fe80::1]" to HostKind.PRIVATE,
        "[fd00::1]" to HostKind.PRIVATE,
        "[fec0::1]" to HostKind.PRIVATE,
        "[::ffff:10.0.0.1]" to HostKind.PRIVATE,
        "example.com" to HostKind.PUBLIC,
        "repo1.maven.org" to HostKind.PUBLIC,
        "8.8.8.8" to HostKind.PUBLIC,
        "100.128.0.1" to HostKind.PUBLIC,
        "[2606:4700::1]" to HostKind.PUBLIC,
        "127.0.0.1.nip.io" to HostKind.PUBLIC,
        "2130706433" to HostKind.AMBIGUOUS,
        "0x7f000001" to HostKind.AMBIGUOUS,
        "127.1" to HostKind.AMBIGUOUS,
        "0177.0.0.1" to HostKind.AMBIGUOUS,
        "0x7f.0.0.1" to HostKind.AMBIGUOUS,
        "127.000.000.001" to HostKind.AMBIGUOUS,
        "256.0.0.1" to HostKind.AMBIGUOUS,
        "example.0x1" to HostKind.AMBIGUOUS,
        "[::zz]" to HostKind.AMBIGUOUS
    ).forEach { (host, kind) ->
        test("classifies $host as $kind") {
            HostClassifier.classify(host) shouldBe kind
        }
    }
})
