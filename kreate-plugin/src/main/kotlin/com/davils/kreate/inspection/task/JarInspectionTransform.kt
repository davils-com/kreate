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
package com.davils.kreate.inspection.task

import com.davils.kreate.inspection.scan.Hit
import com.davils.kreate.inspection.scan.HitsJson
import com.davils.kreate.inspection.scan.JarScanner
import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

private const val MAX_JAR_BYTES: Long = 256L * 1024 * 1024

@CacheableTransform
internal abstract class JarInspectionTransform : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val jar = inputArtifact.get().asFile
        val output = outputs.file("${jar.nameWithoutExtension}.inspection.json")
        output.writeText(HitsJson.render(hitsOf(jar)))
    }

    private fun hitsOf(jar: File): List<Hit> {
        if (jar.length() > MAX_JAR_BYTES) return listOf(Hit("MALFORMED_ARCHIVE", jar.name, "too large to inspect"))
        return JarScanner().scan(jar.readBytes(), jar.name)
    }
}
