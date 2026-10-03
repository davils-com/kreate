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

import com.davils.buildlogic.Project
import dev.detekt.gradle.Detekt

plugins {
    id("dev.detekt")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val qualityConfigDir: File = rootProject.file("../config/detekt")

detekt {
    config.setFrom(qualityConfigDir.resolve("detekt.yml"))
    buildUponDefaultConfig = true
    allRules = true
    ignoreFailures = false
    parallel = true
}

dependencies {
    "detektPlugins"(libs.findLibrary("detekt-rules-ktlintWrapper").get())
    "detektPlugins"(libs.findLibrary("detekt-rules-libraries").get())
    "detektPlugins"(Project.Quality.KREATE_RULES_MODULE)
}

tasks.withType<Detekt>().configureEach {
    reports {
        sarif.required = true
        html.required = true
        checkstyle.required = true
        markdown.required = false
    }
}

apiValidation {
    nonPublicMarkers += "com.davils.kreate.settings.InternalKreateApi"
}
