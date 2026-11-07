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
package com.davils.kreate.governance.policy

internal val DEFAULT_TEST_LIBRARIES: Set<String> = setOf(
    "junit",
    "org.junit",
    "org.mockito",
    "io.mockk",
    "io.kotest",
    "org.assertj",
    "org.hamcrest",
    "org.testng",
    "org.spockframework",
    "com.google.truth",
    "org.testcontainers",
    "org.jetbrains.kotlin:kotlin-test",
    "org.jetbrains.kotlin:kotlin-test-junit",
    "org.jetbrains.kotlin:kotlin-test-junit5",
    "org.jetbrains.kotlinx:kotlinx-coroutines-test"
)

internal data class PolicySettings(
    val denied: Map<String, String> = emptyMap(),
    val allowed: Set<String> = emptySet(),
    val allowedRepositoryHosts: Set<String> = emptySet(),
    val forbidDynamicVersions: Boolean = true,
    val forbidChangingVersions: Boolean = true,
    val testLibraries: Set<String> = DEFAULT_TEST_LIBRARIES,
    val requireLocking: Boolean = true,
    val requireVerification: Boolean = true
)
