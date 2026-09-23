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

plugins {
    alias(libs.plugins.kotlinJvm)
    id("kreate.kotlin-conventions")
    id("kreate.quality-conventions")
    id("kreate.coverage-conventions")
    id("kreate.publish-conventions")
}

dependencies {
    // `compileOnly`, which is how every Detekt rule set is built: Detekt loads the rule set into a
    // class loader that already holds `detekt-api` and the Kotlin compiler frontend. Shipping them
    // again would put two copies of the PSI classes on that classpath, and a `KtFile` from one is
    // not a `KtFile` from the other.
    compileOnly(libs.detekt.api)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.kreate.test)
    testImplementation(libs.detekt.api)
    testImplementation(libs.detekt.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
