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

package com.davils.kreate.locking

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

/**
 * Extension for configuring Gradle dependency locking.
 *
 * Locking pins every resolved dependency version in a `gradle.lockfile`, which is what
 * turns a build into a reproducible one and what gives the Trivy scans something concrete
 * to read. Without a lock file those scans have no version list to check and report
 * success without having checked anything.
 *
 * @param factory The object factory used to create the properties.
 * @since 2.1.0
 */
public abstract class DependencyLockingExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * Whether Kreate activates dependency locking for this project.
     *
     * @since 2.1.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The names of the configurations that are locked.
     *
     * Defaults to the two classpaths that actually ship: `compileClasspath` and
     * `runtimeClasspath`.
     *
     * Under the Kotlin Multiplatform plugin neither of those configurations exists - a classpath
     * is named after its target, `jvmRuntimeClasspath` and `wasmJsCompileClasspath` and so on. The
     * per-target classpaths are therefore derived from the declared targets and locked in addition
     * to whatever is named here, so that a multiplatform project is locked without having to
     * restate its own target list. Naming a configuration that does not exist is harmless; it
     * simply never matches.
     *
     * @since 2.1.0
     */
    public val lockedClasspaths: SetProperty<String> = factory.setProperty(String::class.java)
        .convention(setOf("compileClasspath", "runtimeClasspath"))

    /**
     * Whether every configuration is locked instead of only [lockedClasspaths].
     *
     * Defaults to `false`. Locking all configurations also locks build tool internals —
     * the Kotlin compiler classpath, Dokka's generator, Detekt's rule set plugins — and a
     * vulnerability scan over that lock file reports CVEs in a documentation tool's XML
     * parser as though they were vulnerabilities in the published artifact. In the
     * measurement that produced this default, 2 of 103 locked entries were dependencies
     * the project actually shipped.
     *
     * @since 2.1.0
     */
    public val lockAllConfigurations: Property<Boolean> =
        factory.property(Boolean::class.java).convention(false)
}
