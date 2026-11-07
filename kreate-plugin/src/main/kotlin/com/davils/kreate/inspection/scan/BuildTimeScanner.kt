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
package com.davils.kreate.inspection.scan

import com.davils.kreate.archive.ArchiveEntry
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.jar.Attributes
import java.util.jar.Manifest

private const val MANIFEST_PATH: String = "META-INF/MANIFEST.MF"

private const val SERVICE_PREFIX: String = "META-INF/services/"

private const val COMPILER_PLUGIN_RULE: String = "COMPILER_PLUGIN"

private const val KOTLIN_PLUGIN_PREFIX: String = "org.jetbrains.kotlin.compiler.plugin."

private const val GRADLE_PLUGIN_PREFIX: String = "META-INF/gradle-plugins/"

private val SERVICE_RULES: Map<String, String> = mapOf(
    SERVICE_PREFIX + "javax.annotation.processing.Processor" to "ANNOTATION_PROCESSOR",
    SERVICE_PREFIX + "com.google.devtools.ksp.processing.SymbolProcessorProvider" to COMPILER_PLUGIN_RULE,
    SERVICE_PREFIX + KOTLIN_PLUGIN_PREFIX + "CompilerPluginRegistrar" to COMPILER_PLUGIN_RULE,
    SERVICE_PREFIX + KOTLIN_PLUGIN_PREFIX + "ComponentRegistrar" to COMPILER_PLUGIN_RULE
)

private val PLATFORM_PACKAGES: List<String> = listOf("java.", "kotlin.", "org.gradle.")

private val AGENT_ATTRIBUTES: List<String> = listOf("Premain-Class", "Agent-Class", "Launcher-Agent-Class")

internal object BuildTimeScanner {
    fun scan(entry: ArchiveEntry, hits: MutableCollection<Hit>) {
        val name = entry.name
        SERVICE_RULES[name]?.let { rule -> hits += Hit(rule, name) }
        if (name.startsWith(GRADLE_PLUGIN_PREFIX)) hits += Hit("GRADLE_PLUGIN_DESCRIPTOR", name)
        if (name == MANIFEST_PATH) scanManifest(entry, hits)
        val className = name.replace('/', '.')
        if (name.endsWith(".class") && PLATFORM_PACKAGES.any { prefix -> className.startsWith(prefix) }) {
            hits += Hit("SHADOWED_PLATFORM_CLASS", name)
        }
    }

    private fun scanManifest(entry: ArchiveEntry, hits: MutableCollection<Hit>) {
        val attributes = mainAttributesOf(entry.bytes) ?: return hits.addMalformed(entry.name)
        if (AGENT_ATTRIBUTES.any { name -> attributes.getValue(name) != null }) hits += Hit("JAVA_AGENT", entry.name)
        if (attributes.getValue("Class-Path") != null) hits += Hit("MANIFEST_CLASS_PATH", entry.name)
    }

    private fun mainAttributesOf(bytes: ByteArray): Attributes? = try {
        Manifest(ByteArrayInputStream(bytes)).mainAttributes
    } catch (_: IOException) {
        null
    }

    private fun MutableCollection<Hit>.addMalformed(path: String) {
        add(Hit("MALFORMED_ARCHIVE", path))
    }
}
