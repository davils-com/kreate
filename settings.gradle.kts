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
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Repositories are declared centrally rather than injected into every project by the
    // plugin itself, which is what an enterprise build with an internal mirror expects.
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "kreate"
includeBuild("kreate-settings")
includeBuild("kreate-plugin")

// Included rather than merely published: `:example` puts the rule set on its `detektPlugins`
// configuration, and dependency substitution is what makes it resolve the working copy instead of
// a version that does not exist on Maven Central until the release is cut.
includeBuild("kreate-detekt-rules")
include(":example")
