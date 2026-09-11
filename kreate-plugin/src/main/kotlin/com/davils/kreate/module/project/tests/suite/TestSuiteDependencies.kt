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

package com.davils.kreate.module.project.tests.suite

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import javax.inject.Inject

/**
 * The dependencies of one test suite.
 *
 * A suite's configurations do not exist while the build script is being evaluated - Kreate
 * creates them once it knows which suites there are, which is after the script has run - so a
 * `dependencies { }` block at the top level cannot name them. That is not a Kreate quirk;
 * Gradle's own `JvmTestSuite` carries its dependencies for the same reason, and this block is
 * the same answer: the coordinates are recorded here and added to the configuration at the
 * moment it is created.
 *
 * Kreate declares nothing of its own here. An integration suite's Testcontainers modules, its
 * drivers and its clients are the project's decision and the project's versions, and they are
 * written out in full.
 *
 * On a multiplatform project these land on the suite's shared source set, which is where code
 * common to every JVM target the suite covers is compiled.
 *
 * @param factory The object factory used for creating properties.
 * @since 3.0.0
 */
public abstract class TestSuiteDependencies @Inject constructor(
    /**
     * The object factory instance.
     * @since 3.0.0
     */
    factory: ObjectFactory
) {
    /**
     * The declared `implementation` dependencies.
     *
     * Held apart from the [implementation] function because a property and a function of the
     * same name cannot coexist: `implementation("...")` would resolve to an attempt to invoke
     * the property.
     *
     * @since 3.0.0
     */
    internal val implementationDependencies: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())

    /**
     * The declared `compileOnly` dependencies.
     *
     * @since 3.0.0
     */
    internal val compileOnlyDependencies: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())

    /**
     * The declared `runtimeOnly` dependencies.
     *
     * @since 3.0.0
     */
    internal val runtimeOnlyDependencies: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())

    /**
     * The declared bills of materials.
     *
     * @since 3.0.0
     */
    internal val platformDependencies: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())

    /**
     * Adds a dependency to the suite's compile and runtime classpath.
     *
     * @param dependency The dependency notation, such as `org.testcontainers:postgresql:1.20.4`.
     * @since 3.0.0
     */
    public fun implementation(dependency: String) {
        implementationDependencies.add(dependency)
    }

    /**
     * Adds a dependency to the suite's compile classpath.
     *
     * @param dependency The dependency notation.
     * @since 3.0.0
     */
    public fun compileOnly(dependency: String) {
        compileOnlyDependencies.add(dependency)
    }

    /**
     * Adds a dependency to the suite's runtime classpath.
     *
     * The place for a driver or a logging backend the tests never reference by name.
     *
     * @param dependency The dependency notation.
     * @since 3.0.0
     */
    public fun runtimeOnly(dependency: String) {
        runtimeOnlyDependencies.add(dependency)
    }

    /**
     * Adds a bill of materials to the suite's classpath.
     *
     * @param dependency The platform notation, such as `org.testcontainers:testcontainers-bom:1.20.4`.
     * @since 3.0.0
     */
    public fun platform(dependency: String) {
        platformDependencies.add(dependency)
    }
}
