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
package com.davils.kreate.assurance

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

private const val DEFAULT_FAIL_SCORE: Int = 30

/**
 * Configuration of the inspection of what the resolved dependencies contain.
 *
 * Single findings only inform, because many sound libraries start processes or open sockets. What
 * can stop a build is the risk score of a module, and only with [failOnRisk].
 *
 * @param factory The object factory used to create the properties.
 * @since 4.0.0
 */
public abstract class AssuranceInspectionExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * Whether the dependencies are inspected. Defaults to `false`.
     *
     * @since 4.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * Whether a module at [failScore] blocks the gate. Defaults to `false`, which reports it.
     *
     * @since 4.0.0
     */
    public val failOnRisk: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The risk score from which on a module is reported, out of 100. Defaults to 30.
     *
     * @since 4.0.0
     */
    public val failScore: Property<Int> = factory.property(Int::class.java).convention(DEFAULT_FAIL_SCORE)

    /**
     * Hosts an address in a dependency may point to, in addition to the well known ones.
     *
     * @since 4.0.0
     */
    public val trustedHosts: SetProperty<String> = factory.setProperty(String::class.java)

    /**
     * The configurations to inspect. Defaults to the classpaths of the main code.
     *
     * @since 4.0.0
     */
    public val configurations: SetProperty<String> = factory.setProperty(String::class.java)
}
