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

package com.davils.kreate.module.project

import com.davils.kreate.KreateExtension
import com.davils.kreate.module.project.benchmark.BENCHMARK_PLUGIN_ID
import com.davils.kreate.module.project.coverage.KOVER_PLUGIN_ID
import com.davils.kreate.module.project.detekt.DETEKT_PLUGIN_ID
import com.davils.kreate.module.project.publish.MAVEN_CENTRAL_PLUGIN_ID
import com.davils.kreate.module.project.publish.MAVEN_PUBLISH_PLUGIN_ID
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.jetbrains.kotlinx.serialization.gradle.SerializationGradleSubplugin

/**
 * Applies the optional default Gradle plugins, when the consumer opted in.
 *
 * Only the Kotlin serialization compiler plugin falls into this category. It is opt-in
 * because a compiler plugin is not free, and until 2.0.0 every Kreate project paid for it
 * whether or not it serialized anything.
 *
 * @param projectExtension The project configuration extension.
 * @return Unit
 * @since 1.0.0
 */
internal fun Project.applyDefaultGradlePlugins(projectExtension: ProjectExtension) {
    if (!projectExtension.applySerializationPlugin.get()) return

    pluginManager.apply(SerializationGradleSubplugin::class)
}

/**
 * Applies the third-party plugin behind every feature the consumer enabled.
 *
 * Until 3.0.0 Kreate configured these plugins and refused to run unless the consumer had
 * applied them, on the grounds that it kept the versions out of Kreate's release cycle. In
 * practice it meant a build script listing the same six plugins in every repository, each with
 * a version to keep in step, to use features that are already spelled out one block below in
 * `kreate { }`. Enabling a feature is the decision; applying its plugin is bookkeeping.
 *
 * Applying is idempotent, so a consumer who declares a plugin themselves - to pin a version, or
 * because they configure it beyond what Kreate exposes - keeps exactly that. The version on
 * their `plugins { }` block participates in buildscript classpath resolution like any other
 * dependency.
 *
 * The Kotlin plugin is the deliberate exception and stays the consumer's to apply: which Kotlin
 * plugin a project uses, JVM or multiplatform, is the shape of the project rather than a Kreate
 * feature, and its version governs the language the sources are written in.
 *
 * Applied by plugin id rather than class, because an id is the name the consumer would have
 * written and the one that appears in a build scan.
 *
 * @param extension The Kreate configuration extension.
 * @since 3.0.0
 */
internal fun Project.applyFeaturePlugins(extension: KreateExtension) {
    val projectExtension = extension.project

    if (projectExtension.detekt.enabled.get()) pluginManager.apply(DETEKT_PLUGIN_ID)
    if (projectExtension.coverage.enabled.get()) pluginManager.apply(KOVER_PLUGIN_ID)
    if (projectExtension.benchmark.enabled.get()) pluginManager.apply(BENCHMARK_PLUGIN_ID)

    if (projectExtension.publish.enabled.get()) {
        // Gradle's own plugin, which owns the `publishing { }` extension both repository
        // targets write to. The Maven Central plugin applies it as well; it is named here so
        // that a GitLab-only project gets it too.
        pluginManager.apply(MAVEN_PUBLISH_PLUGIN_ID)

        if (projectExtension.publish.repositories.mavenCentral.enabled.get()) {
            pluginManager.apply(MAVEN_CENTRAL_PLUGIN_ID)
        }
    }
}
