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
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    // `java` rather than `java-gradle-plugin`. `kreate-plugin` gets the latter from `kotlin-dsl`,
    // and `kreate-detekt-rules` must not have it at all: it adds `gradleApi()` to the `api`
    // configuration, which would put the Gradle API into the POM of a rule set JAR that is
    // resolved onto Detekt's analysis classpath.
    java
}

java {
    val target = JavaVersion.toVersion(Project.Compatibility.TARGET_JAVA_VERSION)
    sourceCompatibility = target
    targetCompatibility = target
}

plugins.withId("org.jetbrains.kotlin.jvm") {
    extensions.configure<KotlinJvmProjectExtension> {
        explicitApi()
        jvmToolchain(Project.Compatibility.TARGET_JAVA_VERSION)

        compilerOptions {
            allWarningsAsErrors = true
        }
    }
}

// Task property validation is the plugin author's equivalent of a type checker: it is what
// catches missing input annotations, outputs nested inside inputs, and other mistakes that
// otherwise surface as stale build outputs on a user's machine.
tasks.withType<ValidatePlugins>().configureEach {
    failOnWarning = true
    enableStricterValidation = true
}

// Reproducible archives: identical sources must produce byte-identical artifacts.
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    dirPermissions { unix("rwxr-xr-x") }
    filePermissions { unix("rw-r--r--") }
}

// The plugin has to be able to name its own version at runtime — it records it alongside every
// local publication, so that a record left behind by an older Kreate can be recognised as such.
// The manifest is the only place that survives into the published artefact.
//
// Both values are derived from the build's own inputs, so reproducibility above is unaffected.
val manifestVersion = provider { project.version.toString() }
val manifestTitle = Project.Identity.NAME

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            "Implementation-Title" to manifestTitle,
            "Implementation-Version" to manifestVersion
        )
    }
}
