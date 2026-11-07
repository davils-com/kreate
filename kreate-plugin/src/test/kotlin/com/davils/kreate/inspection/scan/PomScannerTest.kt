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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class PomScannerTest : FunSpec({
    context("a POM") {
        test("tells where a module moved to") {
            val pom = """
                <project><distributionManagement><relocation>
                  <groupId>org.evil</groupId><artifactId>lib</artifactId>
                </relocation></distributionManagement></project>
            """.trimIndent()

            val hit = PomScanner.scan(pom.toByteArray()).single()

            hit.rule shouldBe "POM_RELOCATED"
            hit.detail shouldBe "org.evil:lib"
        }

        test("names a repository it brings along") {
            val pom = """
                <project><repositories><repository><url>http://repo.evil.example</url></repository></repositories></project>
            """.trimIndent()

            val hit = PomScanner.scan(pom.toByteArray()).single()

            hit.rule shouldBe "POM_REPOSITORY"
            hit.detail shouldBe "http://repo.evil.example"
        }

        test("is quiet about an ordinary one") {
            PomScanner.scan("<project><artifactId>a</artifactId></project>".toByteArray()).shouldBeEmpty()
        }

        test("does not resolve an external entity") {
            val pom = """<!DOCTYPE p [<!ENTITY e SYSTEM "file:///etc/passwd">]><project>&e;</project>"""

            PomScanner.scan(pom.toByteArray()).shouldBeEmpty()
        }

        test("survives what is not XML") {
            PomScanner.scan("nonsense".toByteArray()).shouldBeEmpty()
        }
    }
})
