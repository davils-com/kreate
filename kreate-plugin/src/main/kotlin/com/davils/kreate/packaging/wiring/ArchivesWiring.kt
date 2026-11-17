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

package com.davils.kreate.packaging.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.packaging.ArchivesExtension
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.withType

private val MAIN_JARS: Set<String> = setOf("jar", "jvmJar")

private const val META_INF: String = "META-INF"

private const val IMPLEMENTATION_TITLE: String = "Implementation-Title"

private const val IMPLEMENTATION_VERSION: String = "Implementation-Version"

private const val IMPLEMENTATION_VENDOR: String = "Implementation-Vendor"

internal fun Project.configureArchives(extension: KreateExtension) {
    val archives = extension.project.archives
    val legalFiles = archives.legalFiles
    if (!legalFiles.isEmpty) addLegalFiles(archives)
    if (archives.implementationManifest.get()) nameImplementation(extension)
}

private fun Project.addLegalFiles(archives: ArchivesExtension) {
    val legalFiles = archives.legalFiles
    mainJars().configureEach {
        from(legalFiles) { into(META_INF) }
    }
}

private fun Project.nameImplementation(extension: KreateExtension) {
    val owner = this
    val moduleName = extension.platform.jvm.moduleName
    val title = moduleName.orElse(name).get()
    val vendor = extension.project.archives.vendor.orNull
    val version = provider { owner.version.toString() }
    val attributes = buildMap {
        put(IMPLEMENTATION_TITLE, title)
        put(IMPLEMENTATION_VERSION, version)
        vendor?.let { name -> put(IMPLEMENTATION_VENDOR, name) }
    }

    mainJars().configureEach { manifest.attributes(attributes) }
}

private fun Project.mainJars() = tasks.withType<Jar>().matching { task -> task.name in MAIN_JARS }
