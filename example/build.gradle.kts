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

import com.davils.kreate.cinterop.NativeLanguage
import com.davils.kreate.coverage.Grouping
import com.davils.kreate.testing.KotestModule
import com.davils.kreate.testing.LegacyTestPolicy
import com.davils.kreate.trivy.LicenseSeverity
import com.davils.kreate.trivy.Score
import com.davils.kreate.trivy.SecretSeverity
import java.time.Year

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kreate)
    application
}

application {
    mainClass = "com.davils.example.JNIKt"
}

group = "com.example"

kreate {
    platform {
        javaVersion = JavaVersion.VERSION_17
        explicitApi = true
        allWarningsAsErrors = false

        multiplatform {
            cInterop {
                enabled = false
                language = NativeLanguage.RUST
                nameOverride = "example"
                projectDirectory = file("cinterop")
                packageNameOverride.set("com.davils.example.cinterop")
                rustTargets = listOf("x86_64-unknown-linux-gnu")

                defFile {
                    fileName = "cinterop.def"
                    dirName = "defs"
                }


            }
        }

        jvm {
            jni {
                enabled = true
                projectDirectory = layout.projectDirectory.dir("jni")
                nameOverride = "example"
                buildType = "Release"
                libraryRuntimePaths = listOf()
                libraryIncludePaths = listOf()

                headers {
                    enabled = true
                }

                packaging {
                    enabled = true
                    generateLoader = true
                }
            }
        }
    }

    trivy {
        enabled = true

        vulnerability {
            score = listOf(Score.CRITICAL, Score.HIGH, Score.MEDIUM, Score.LOW)
            failOnFindings = true
            lockFiles.from(
                fileTree(projectDir) {
                    include("*.lockfile")
                }
            )
        }

        license {
            severity = listOf(LicenseSeverity.CRITICAL, LicenseSeverity.HIGH, LicenseSeverity.UNKNOWN)
            failOnForbidden = true
            ignoredLicenses = listOf("MIT")
            lockFiles.from(
                fileTree(projectDir) {
                    include("*.lockfile")
                }
            )
        }

        secrets {
            severity = listOf(SecretSeverity.CRITICAL, SecretSeverity.HIGH, SecretSeverity.MEDIUM, SecretSeverity.LOW)
            failOnFindings = true
            secretConfig = rootProject.layout.projectDirectory.file("trivy-secret.yaml")
            sourceFiles.setFrom(
                fileTree(projectDir) {
                    include("src/**/*.kt", "src/**/*.java", "**/*.yaml", "**/*.yml", "**/*.env", "**/*.properties", "**/*.json")
                    exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**")
                }
            )
        }
    }

    project {
        name = "Example"
        description = "Example project"

        dependencyLocking {
            enabled = true
        }

        apiValidation {
            enabled = true
        }

        benchmark {
            enabled = true

            profiles {
                named("main") {
                    warmups = 1
                    iterations = 2
                    iterationTime = 250
                    iterationTimeUnit = "ms"
                    advanced("jvmForks", "1")
                }
            }

            regression {
                maxRegressionPercent = 75.0
            }
        }

        version {
            environment = "CI_COMMIT_TAG"
            property = "version"
        }

        buildConstant {
            enabled = true
            className = "ExampleConstants"
            path = "generated/compile"

            constant("example", "value")
            constant("example2", 1)
        }

        docs {
            enabled = true
            outputDirectory = "docs"
            moduleName = "Example"
            copyright = "Copyright ${Year.now()} Example"
        }

        tests {
            enabled = true
            maxParallelForks = Runtime.getRuntime().availableProcessors()
            timeoutMinutes = 10L
            ignoreFailures = false
            alwaysRunTests = false
            failOnNoDiscoveredTests = false

            logging {
                logPassedTests = true
                logSkippedTests = true
                logTestStarted = true
            }

            report {
                enabled = true
                xml = true
                html = true
            }

            legacyTestSourceSet = LegacyTestPolicy.FAIL

            kotest {
                enabled = true
                modules = listOf(KotestModule.ASSERTIONS)
            }

            suites {
                named("unitTest") {
                    maxParallelForks = Runtime.getRuntime().availableProcessors()
                }

                named("integrationTest") {
                    runOnCheck = false
                    mustRunAfterSuites = listOf("unitTest")

                    maxParallelForks = 1
                    timeoutMinutes = 30L

                    environment = mapOf("EXAMPLE_INTEGRATION" to "true")
                }

                register("contractTest") {
                    runOnCheck = false
                    includeTags = listOf("contract")
                }
            }
        }

        detekt {
            enabled = true
            buildUponDefaultConfig = true
            allRules = true
            config = rootProject.file("config/detekt/detekt-consumer.yml")

            reports {
                checkstyle {
                    required = true
                    outputLocation = layout.buildDirectory.file("reports/detekt/checkstyle.xml")
                }

                html {
                    required = true
                    outputLocation = layout.buildDirectory.file("reports/detekt/html.html")
                }

                markdown {
                    required = true
                    outputLocation = layout.buildDirectory.file("reports/detekt/markdown.md")
                }

                sarif {
                    required = true
                    outputLocation = layout.buildDirectory.file("reports/detekt/sarif.sarif")
                }
            }
        }

        coverage {
            enabled = true

            sources {
                excludedSourceSets = listOf("benchmarks")
            }

            filters {
                excludes {
                    classes = listOf(
                        "com.davils.example.ExampleConstants",
                        "com.example.example.jni.KreateNativeLoader",
                        "com.davils.example.JNI",
                        "com.davils.example.JNIKt"
                    )
                }
            }

            reports {
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
            }

            verify {
                runOnCheck = true
                warningInsteadOfFailure = false
                groupBy = Grouping.APPLICATION
                minLineCoverage = 60
                minBranchCoverage = 50
            }
        }

        publish {
            enabled = false
            inceptionYear = 2026
            website = "https://example.com"

            pom {
                issueManagement {
                    system = "Github Issues"
                    url = "https://github.com/example/issues"
                }

                ciManagement {
                    system = "Github Actions"
                    url = "https://github.com/example/actions"
                }

                licenses {
                    license {
                        name = "Apache 2.0"
                        url = "https://github.com/example/example-project/blob/main/LICENSE"
                        distribution = "repo"
                    }
                }

                developers {
                    developer {
                        id = "example"
                        name = "Example"
                        email = "example@example.com"
                        organization = "Example"
                        timezone = "Europe/Berlin"
                    }
                }

                scm {
                    url = "https://github.com/example/example-project.git"
                    connection = "scm:git:https://github.com/example/example-project.git"
                    developerConnection = "scm:git:ssh://git@github.com:example/example-project.git"
                }
            }

            repositories {
                gitlab {
                    enabled = true
                    name = "ExampleInstance"
                    tokenEnv = "CI_JOB_TOKEN"
                    projectIdEnv = "CI_PROJECT_ID"
                    apiUrlEnv = "CI_API_V4_URL"
                }

                mavenCentral {
                    enabled = true
                    automaticRelease = true
                    signPublications = true
                }
            }
        }
    }
}
