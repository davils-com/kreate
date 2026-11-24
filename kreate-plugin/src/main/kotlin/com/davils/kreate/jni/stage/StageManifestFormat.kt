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

package com.davils.kreate.jni.stage

import com.davils.kore.core.text.SafeText
import java.io.StringReader
import java.util.Properties

internal const val STAGE_MANIFEST_FILE_NAME: String = "native.properties"

private const val FORMAT_VERSION: String = "1"

private const val FORMAT_KEY: String = "format"

private const val PLATFORM_KEY: String = "platform"

private const val GROUP_KEY: String = "group"

private const val ARTIFACT_KEY: String = "artifact"

private const val VERSION_KEY: String = "version"

private const val REVISION_KEY: String = "revision"

private const val DIGEST_PREFIX: String = "sha256."

private const val BACKSLASH: String = "\\"

private val LIBRARY_NAME: Regex = Regex("^[A-Za-z0-9._+-]+$")

private val SHA256: Regex = Regex("^[0-9a-f]{64}$")

internal object StageManifestFormat {
    fun render(manifest: StageManifest): String = buildString {
        val revision = manifest.revision
        appendLine("# Written by kreateJniStage. Do not edit - kreateJniVerifyPlatforms checks every line.")
        appendLine("$FORMAT_KEY=$FORMAT_VERSION")
        appendLine("$PLATFORM_KEY=${escaped(manifest.platform)}")
        appendLine("$GROUP_KEY=${escaped(manifest.group)}")
        appendLine("$ARTIFACT_KEY=${escaped(manifest.artifact)}")
        appendLine("$VERSION_KEY=${escaped(manifest.version)}")
        if (revision != null) appendLine("$REVISION_KEY=${escaped(revision)}")
        for ((name, digest) in manifest.digests.toSortedMap()) {
            appendLine("$DIGEST_PREFIX${requireLibraryName(name)}=$digest")
        }
    }

    fun parse(text: String): StageManifest {
        val properties = Properties()
        properties.load(StringReader(text))
        val format = properties.getProperty(FORMAT_KEY)
        if (format != FORMAT_VERSION) {
            throw StageManifestException("has the format ${shown(format)}, but Kreate reads format $FORMAT_VERSION")
        }

        return StageManifest(
            platform = required(properties, PLATFORM_KEY),
            group = required(properties, GROUP_KEY),
            artifact = required(properties, ARTIFACT_KEY),
            version = required(properties, VERSION_KEY),
            revision = properties.getProperty(REVISION_KEY),
            digests = digestsOf(properties)
        )
    }

    fun isLibraryName(name: String): Boolean = LIBRARY_NAME.matches(name)

    private fun required(properties: Properties, key: String): String {
        val value = properties.getProperty(key)
        if (value.isNullOrBlank()) throw StageManifestException("has no `$key`")
        return value
    }

    private fun digestsOf(properties: Properties): Map<String, String> {
        val names = properties.stringPropertyNames()
        val digestKeys = names.filter { key -> key.startsWith(DIGEST_PREFIX) }
        if (digestKeys.isEmpty()) throw StageManifestException("lists no library")
        return digestKeys.associate { key -> digestEntryOf(key, properties.getProperty(key)) }
    }

    private fun digestEntryOf(key: String, digest: String): Pair<String, String> {
        val name = key.removePrefix(DIGEST_PREFIX)
        if (!isLibraryName(name)) throw StageManifestException("lists the file name ${shown(name)}")
        if (!SHA256.matches(digest)) throw StageManifestException("gives ${shown(name)} no SHA-256 digest")
        return name to digest
    }

    private fun shown(value: String?): String = SafeText.quoted(value.orEmpty())
}

private fun escaped(value: String): String {
    val hasControlCharacter = value.any { character -> character.isISOControl() }
    if (hasControlCharacter) throw StageManifestException("cannot hold a value with a control character")
    return value.replace(BACKSLASH, BACKSLASH + BACKSLASH)
}

private fun requireLibraryName(name: String): String {
    if (StageManifestFormat.isLibraryName(name)) return name
    throw StageManifestException("cannot list the file name ${SafeText.quoted(name)}")
}
