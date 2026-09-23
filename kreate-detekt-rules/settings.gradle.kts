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

// A build of its own rather than a subproject of `kreate-plugin`, for one reason: the rule set is
// a plain library that ends up on Detekt's analysis classpath, while `kreate-plugin` is built with
// `kotlin-dsl` and carries the Gradle API. Sharing a build would mean sharing that build script
// classpath, and a rule set JAR that drags the Gradle API behind it is not a rule set anyone can
// put on a `detektPlugins` configuration.
rootProject.name = "kreate-detekt-rules"
