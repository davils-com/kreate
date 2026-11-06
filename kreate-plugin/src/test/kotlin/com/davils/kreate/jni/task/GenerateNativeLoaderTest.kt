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
package com.davils.kreate.jni.task

import com.davils.kreate.freshDirectory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder

private const val LOADER_FILE: String = "com/example/jni/KreateNativeLoader.kt"

private const val DEFAULT_COORDINATE: String = "com.example:sample:1.0.0"

class GenerateNativeLoaderTest : FunSpec({
    val workspace = tempdir()

    fun loaderTask(
        packageName: String = "com.example.jni",
        resourcePath: String = "native",
        coordinate: String = DEFAULT_COORDINATE
    ): GenerateNativeLoader {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        val task = project.tasks.register("generateLoader", GenerateNativeLoader::class.java).get()
        task.packageName.set(packageName)
        task.resourcePath.set(resourcePath)
        task.artifactCoordinate.set(coordinate)
        task.publishedPlatforms.set(listOf("linux-x86_64"))
        task.outputDirectory.set(workspace.freshDirectory("output"))
        return task
    }

    fun renderedLoader(task: GenerateNativeLoader): String {
        task.execute()
        val output = task.outputDirectory.get()
        return output.file(LOADER_FILE).asFile.readText()
    }

    context("GenerateNativeLoader") {
        test("looks for the packaged copy before it touches the library path") {
            val loader = renderedLoader(loaderTask())

            loader.indexOf("getResourceAsStream") shouldBeLessThan loader.indexOf("System.loadLibrary(name)")
        }

        test("reads the library path only behind the explicit system property") {
            val loader = renderedLoader(loaderTask())

            val guard = loader.indexOf("java.lang.Boolean.getBoolean(property)")
            guard shouldBeLessThan loader.indexOf("System.loadLibrary(name)")
            loader shouldContain "\"$JNI_USE_LIBRARY_PATH_PROPERTY\""
        }

        test("records a library as loaded only after loading it succeeded") {
            val loader = renderedLoader(loaderTask())

            loader.indexOf("loadPackaged(name, stream)") shouldBeLessThan loader.indexOf("loaded.add(name)")
            loader shouldNotContain "loaded.add(name)) return"
        }

        test("deletes the temporary directory after the library inside it") {
            val loader = renderedLoader(loaderTask())

            loader.indexOf("directory.toFile().deleteOnExit()") shouldBeLessThan
                loader.indexOf("target.toFile().deleteOnExit()")
        }

        test("escapes a quote in the version instead of closing the string literal") {
            val loader = renderedLoader(loaderTask(coordinate = "com.example:sample:1.0\"+evil()+\""))

            loader shouldNotContain ":1.0\"+evil()"
            loader shouldContain ":1.0\\\"+evil()+\\\""
        }

        test("escapes a template expression in the version") {
            val loader = renderedLoader(loaderTask(coordinate = "com.example:sample:\${evil()}"))

            loader shouldNotContain "\":\${evil()}\""
            loader shouldContain "\${'$'}{evil()}"
        }

        test("escapes a quote in the resource path") {
            val loader = renderedLoader(loaderTask(resourcePath = "native\"+evil()+\""))

            loader shouldNotContain "native\"+evil()"
        }

        test("accepts a package name of letters, digits and underscores") {
            val loader = renderedLoader(loaderTask())

            loader shouldContain "package com.example.jni"
        }

        test("refuses a package name that is not one") {
            val task = loaderTask(packageName = "com.example; val evil = 1")

            val failure = shouldThrow<GradleException> { task.execute() }

            failure.message.orEmpty() shouldContain "not a valid package name"
        }

        test("refuses a package name that starts with a separator, as an empty group produces") {
            shouldThrow<GradleException> { loaderTask(packageName = ".sample.jni").execute() }
        }

        test("refuses a control character instead of rendering a raw multi-line string") {
            val task = loaderTask(coordinate = "com.example:sample:1.0\n")

            shouldThrow<GradleException> { task.execute() }
        }

        test("renders an empty hint for a build that publishes no platform artifacts") {
            val loader = renderedLoader(loaderTask(coordinate = ""))

            loader shouldContain "private fun hint(): String = \"\""
            loader.contains("runtimeOnly") shouldBe false
        }
    }
})
