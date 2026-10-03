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

package com.davils.kreate.settings.local

import io.mockk.every
import io.mockk.mockk
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

internal inline fun <reified T : Any> providerOf(value: T?): Provider<T> {
    val provider = mockk<Provider<T>>()
    every { provider.orNull } returns value
    every { provider.isPresent } returns (value != null)
    every { provider.get() } answers { checkNotNull(value) }
    every { provider.getOrElse(any()) } answers { value ?: firstArg() }
    every { provider.orElse(any<Provider<T>>()) } answers { value?.let { provider } ?: firstArg() }
    return provider
}

internal fun fakeProviders(
    gradleProperties: Map<String, String> = emptyMap(),
    environment: Map<String, String> = emptyMap(),
    systemProperties: Map<String, String> = emptyMap(),
    workspace: LocalWorkspace = LocalWorkspace.EMPTY
): ProviderFactory {
    val providers = mockk<ProviderFactory>()
    every { providers.gradleProperty(any<String>()) } answers { providerOf(gradleProperties[firstArg<String>()]) }
    every { providers.environmentVariable(any<String>()) } answers { providerOf(environment[firstArg<String>()]) }
    every { providers.systemProperty(any<String>()) } answers { providerOf(systemProperties[firstArg<String>()]) }
    every { providers.of(LocalWorkspaceSource::class.java, any()) } returns providerOf(workspace)
    return providers
}
