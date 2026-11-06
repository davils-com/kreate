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

package com.davils.kreate

import com.davils.kreate.tooling.ExecOperationsToolRunner
import com.davils.kreate.tooling.ToolRunner
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import org.objectweb.asm.ClassReader
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.streams.toList

private const val GRADLE_PACKAGE: String = "org/gradle"

private const val PACKAGE_ROOT: String = "com/davils/kreate"

private const val CONSTANT_CLASS: Int = 7

private const val CONSTANT_STRING: Int = 8

private const val CONSTANT_NAME_AND_TYPE: Int = 12

private const val CONSTANT_METHOD_TYPE: Int = 16

private val GRADLE_FREE_PACKAGES: List<String> = listOf("net", "archive")

private val GRADLE_FREE_TOOLING_TYPES: List<String> = listOf(
    "ToolRunner",
    "ToolInvocation",
    "ToolResult",
    "ToolOutput",
    "ToolStatus",
    "BoundedOutputStream"
)

private fun classesRoot(): Path {
    val location = ToolRunner::class.java.protectionDomain.codeSource.location
    return File(location.toURI()).toPath()
}

private fun classFilesUnder(directory: Path): List<Path> =
    Files.walk(directory).use { paths ->
        paths.filter { path -> path.isRegularFile() && path.extension == "class" }.toList()
    }

private fun constantPoolReferences(reader: ClassReader): List<String> {
    val buffer = CharArray(reader.maxStringLength)
    return (1 until reader.itemCount).mapNotNull { index -> constantAt(reader, index, buffer) }
}

private fun constantAt(reader: ClassReader, index: Int, buffer: CharArray): String? {
    val offset = reader.getItem(index)
    if (offset == 0) return null

    when (reader.readByte(offset - 1)) {
        CONSTANT_CLASS, CONSTANT_STRING, CONSTANT_METHOD_TYPE -> return reader.readUTF8(offset, buffer)
        CONSTANT_NAME_AND_TYPE -> return reader.readUTF8(offset + 2, buffer)
    }
    return null
}

private fun gradleReferencesOf(classFile: Path): List<String> {
    val reader = ClassReader(classFile.readBytes())
    val collector = GradleReferenceCollector()
    reader.accept(collector, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

    val references = collector.references + constantPoolReferences(reader)
    val gradleReferences = references.filter { reference -> GRADLE_PACKAGE in reference }
    return gradleReferences.map { reference -> "${classFile.fileName}: $reference" }
}

class GradleFreeGuardTest : FunSpec({
    val root = classesRoot()

    GRADLE_FREE_PACKAGES.forEach { packageName ->
        test("the package $packageName references no Gradle type") {
            val classFiles = classFilesUnder(root.resolve("$PACKAGE_ROOT/$packageName"))

            classFiles.shouldNotBeEmpty()
            classFiles.flatMap(::gradleReferencesOf).shouldBeEmpty()
        }
    }

    test("the seam of the tool runner references no Gradle type") {
        val tooling = classFilesUnder(root.resolve("$PACKAGE_ROOT/tooling"))
        val seam = tooling.filter { path ->
            val fileName = path.fileName.toString()
            val className = fileName.substringBefore('.')
            className.substringBefore('$') in GRADLE_FREE_TOOLING_TYPES
        }

        seam.shouldNotBeEmpty()
        seam.flatMap(::gradleReferencesOf).shouldBeEmpty()
    }

    test("the guard notices a class that does reference Gradle") {
        val adapterPath = ExecOperationsToolRunner::class.java.name.replace('.', '/')

        val references = gradleReferencesOf(root.resolve("$adapterPath.class"))

        references.shouldNotBeEmpty()
    }
})
