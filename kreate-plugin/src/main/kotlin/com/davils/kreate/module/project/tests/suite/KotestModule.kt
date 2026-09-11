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

/**
 * A Kotest artifact Kreate can add to a suite on request.
 *
 * The runner is not listed here: it is not a choice, it follows from the platform, and Kreate
 * adds it whenever the bundle is enabled. What remains are the optional libraries, each of
 * which a project either uses or does not.
 *
 * @since 3.0.0
 */
public enum class KotestModule(
    /**
     * The Maven artifact id within the `io.kotest` group.
     * @since 3.0.0
     */
    internal val artifact: String
) {
    /**
     * The matcher library (`kotest-assertions-core`).
     * @since 3.0.0
     */
    ASSERTIONS("kotest-assertions-core"),

    /**
     * Property based testing (`kotest-property`).
     * @since 3.0.0
     */
    PROPERTY("kotest-property"),

    /**
     * Data driven testing (`kotest-framework-datatest`).
     * @since 3.0.0
     */
    DATATEST("kotest-framework-datatest"),

    /**
     * JUnit XML report output (`kotest-extensions-junitxml`).
     * @since 3.0.0
     */
    JUNIT_XML("kotest-extensions-junitxml")
}
