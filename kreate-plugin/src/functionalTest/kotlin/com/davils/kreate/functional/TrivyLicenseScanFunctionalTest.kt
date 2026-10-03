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

package com.davils.kreate.functional

import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import kotlin.io.path.createTempDirectory

private const val SCANNED_COPY: String = "scanned.lockfile"

private const val SHIPPED_DEPENDENCY: String = "com.example:shipped:1.0.0=compileClasspath,runtimeClasspath"

private const val TEST_DEPENDENCY: String = "net.java.dev.jna:jna:5.9.0=unitTestRuntimeClasspath"

class TrivyLicenseScanFunctionalTest : FunSpec({
    val workspace = tempdir()

    fun KreateBuildFixture.writeLicenseBuild(licenseBlock: String) {
        write("gradle.lockfile", "$SHIPPED_DEPENDENCY\n$TEST_DEPENDENCY\nempty=\n")
        writeBuild(
            """
            ${KreateBuildFixture.platformBlock}

            trivy {
                enabled = true

                secrets {
                    runOnCheck = false
                }

                license {
                    lockFiles.setFrom(file("gradle.lockfile"))
                    $licenseBlock
                }
            }
            """.trimIndent()
        )
    }

    fun KreateBuildFixture.installCopyingTrivy(toolDirectory: File) {
        val copy = toolDirectory.resolve(SCANNED_COPY)
        val trivy = toolDirectory.resolve("trivy")
        trivy.writeText(
            """
            #!/bin/sh
            for target; do :; done
            cp "${'$'}target" "${copy.absolutePath}"
            exit 0
            """.trimIndent() + "\n"
        )
        trivy.setExecutable(true)
        withEnvironment("PATH", "${toolDirectory.absolutePath}${File.pathSeparator}${System.getenv("PATH")}")
    }

    fun scannedLockFile(licenseBlock: String): String {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.writeLicenseBuild(licenseBlock)
        val toolDirectory = createTempDirectory(workspace.toPath(), "tool").toFile()
        fixture.installCopyingTrivy(toolDirectory)

        fixture.build("kreateTrivyLicenseScan")

        val copy = toolDirectory.resolve(SCANNED_COPY)
        return copy.readText()
    }

    context("Trivy license scan").config(enabledOrReasonIf = requiresPosixShell) {
        test("checks only the dependencies the main code compiles and runs with") {
            val scanned = scannedLockFile("")

            scanned shouldContain SHIPPED_DEPENDENCY
            scanned shouldNotContain TEST_DEPENDENCY
        }

        test("checks the configurations a project names explicitly") {
            val scanned = scannedLockFile("""configurations.set(setOf("unitTestRuntimeClasspath"))""")

            scanned shouldContain TEST_DEPENDENCY
            scanned shouldNotContain SHIPPED_DEPENDENCY
        }
    }
})
