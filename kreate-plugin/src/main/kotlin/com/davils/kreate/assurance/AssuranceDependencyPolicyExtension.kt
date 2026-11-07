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
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

/**
 * Configuration of the dependency policy: which modules and repositories a build may use, how
 * versions are declared, how the build is hardened and how far dependencies may lag behind.
 *
 * A module is named as `group:artifact`, as `group:*`, or as a group, which includes its subgroups.
 *
 * @param factory The object factory used to create the properties.
 * @since 4.0.0
 */
public abstract class AssuranceDependencyPolicyExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * Whether the policy is checked. Defaults to `false`.
     *
     * @since 4.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * Modules the build must not use, directly or transitively, with the reason.
     *
     * @since 4.0.0
     */
    public val denied: MapProperty<String, String> = factory.mapProperty(String::class.java, String::class.java)

    /**
     * The only modules a build may declare directly. Empty allows every module.
     *
     * @since 4.0.0
     */
    public val allowed: SetProperty<String> = factory.setProperty(String::class.java)

    /**
     * The only hosts repositories may point to. Empty allows every host.
     *
     * @since 4.0.0
     */
    public val allowedRepositoryHosts: SetProperty<String> = factory.setProperty(String::class.java)

    /**
     * Whether versions such as `1.+`, `latest.release` and ranges are reported. Defaults to `true`.
     *
     * @since 4.0.0
     */
    public val forbidDynamicVersions: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * Whether snapshot dependencies are reported. Defaults to `true`.
     *
     * @since 4.0.0
     */
    public val forbidChangingVersions: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * Further test libraries that must not be declared for production code, in addition to JUnit,
     * Kotest, MockK, Mockito, AssertJ and the other built-in ones.
     *
     * @since 4.0.0
     */
    public val testLibraries: SetProperty<String> = factory.setProperty(String::class.java)

    /**
     * Whether a project without a lock file, or with a lenient lock mode, is reported. Defaults to `true`.
     *
     * @since 4.0.0
     */
    public val requireLocking: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * Whether missing dependency verification and an unchecked Gradle distribution are reported.
     * Defaults to `true`.
     *
     * @since 4.0.0
     */
    public val requireVerification: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * Whether `kreateDependencyOutdated` asks deps.dev how far the direct dependencies lag behind.
     * Defaults to `false`.
     *
     * @since 4.0.0
     */
    public val checkOutdated: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The number of major releases a direct dependency may lag behind. Unset reports none.
     *
     * @since 4.0.0
     */
    public val maxMajorBehind: Property<Int> = factory.property(Int::class.java)

    /**
     * The libyears the direct dependencies may lag behind in total. Unset reports none.
     *
     * @since 4.0.0
     */
    public val maxLibyears: Property<Double> = factory.property(Double::class.java)

    /**
     * The configurations to check. Defaults to the classpaths of the main code.
     *
     * @since 4.0.0
     */
    public val configurations: SetProperty<String> = factory.setProperty(String::class.java)

    /**
     * Denies [module] for [reason].
     *
     * @param module The module, as `group:artifact`, `group:*` or a group.
     * @param reason Why the module is denied and what to use instead.
     * @since 4.0.0
     */
    public fun deny(module: String, reason: String) {
        denied.put(module, reason)
    }
}
