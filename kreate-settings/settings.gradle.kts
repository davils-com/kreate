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

pluginManagement {
    includeBuild("../build-logic")

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

// A build of its own rather than part of `kreate-plugin`, for one reason: a settings plugin is
// loaded into the settings class loader, which is the parent of every project class loader in the
// build, and Gradle loads parent first. `kreate-plugin` carries the Kotlin, Dokka, Detekt, Kover and
// publishing plugins as runtime dependencies, so while the settings plugin shipped in the same
// artefact, applying it pinned every one of those plugins at Kreate's version for the whole build -
// a consumer declaring Kotlin 2.4.20 in its own catalog compiled with Kreate's 2.4.0 (ARC-66). This
// artefact depends on the Gradle API and nothing else.
rootProject.name = "kreate-settings"
