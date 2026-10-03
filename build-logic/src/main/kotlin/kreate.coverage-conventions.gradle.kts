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
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    id("org.jetbrains.kotlinx.kover")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val jacocoToolVersion: String = libs.findVersion("jacoco").get().requiredVersion

val minimumLineCoverage: Int = coverageBound("kreate.quality.minimumLineCoverage")
val minimumBranchCoverage: Int = coverageBound("kreate.quality.minimumBranchCoverage")

fun coverageBound(propertyName: String): Int {
    val property = providers.gradleProperty(propertyName)
    val bound = property.map { value -> value.toInt() }
    return bound.getOrElse(Project.Quality.MINIMUM_COVERAGE)
}

kover {
    useJacoco(jacocoToolVersion)

    currentProject {
        sources {
            excludedSourceSets.add("functionalTest")
        }
    }

    reports {
        total {
            xml {
                onCheck = false
            }

            html {
                onCheck = false
            }

            log {
                onCheck = false
                format = "<entity> line coverage: <value>%"
            }

            verify {
                onCheck = true

                rule("Minimum coverage") {
                    minBound(minimumLineCoverage, CoverageUnit.LINE)
                    minBound(minimumBranchCoverage, CoverageUnit.BRANCH)
                }
            }
        }
    }
}
