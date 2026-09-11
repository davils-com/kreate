import com.davils.kreate.module.platform.multiplatform.cinterop.NativeLanguage
import com.davils.kreate.module.project.coverage.Grouping
import com.davils.kreate.module.project.tests.LegacyTestPolicy
import com.davils.kreate.module.project.tests.suite.KotestModule
import com.davils.kreate.module.trivy.LicenseSeverity
import com.davils.kreate.module.trivy.SecretSeverity
import com.davils.kreate.module.trivy.Score
import java.time.Year

plugins {
    kotlin("jvm") version "2.4.20"
    alias(libs.plugins.kreate)
    application
}

application {
    mainClass = "com.davils.example.JNIKt"
}

group = "com.example"

dependencies {
    // Nothing here for the test suites: a suite's configurations do not exist yet while this
    // script runs, so its dependencies are declared in the suite itself.
}

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

                mingw {
                    // Configure MinGW-specific settings here
                }

                linux {
                    // Configure Linux-specific settings here
                }

                macos {
                    // Configure macOS-specific settings here
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
            sourceFiles.from(
                fileTree(projectDir) {
                    include("src/**/*.kt", "src/**/*.java", "**/*.yaml", "**/*.yml", "**/*.env", "**/*.properties", "**/*.json")
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

            // The migration is finished here, so the build fails if anything reappears
            // under src/test. Use ALIAS while moving, which runs an unmoved tree as the
            // unit suite - remembering to move its dependencies onto the suite as well.
            legacyTestSourceSet = LegacyTestPolicy.FAIL

            kotest {
                enabled = false
                modules = listOf(KotestModule.ASSERTIONS)
            }

            suites {
                named("unitTest") {
                    maxParallelForks = Runtime.getRuntime().availableProcessors()

                    dependencies {
                        // The JUnit 5 backed variant, named explicitly: the Kotlin plugin
                        // only infers a variant for the conventional `test` configuration.
                        implementation("org.jetbrains.kotlin:kotlin-test-junit5:2.4.10")
                        implementation("org.junit.jupiter:junit-jupiter:6.1.3")
                    }
                }

                named("integrationTest") {
                    // Left off `check` and ordered after the unit suite, which is what the
                    // defaults already do; spelled out here because this is the example.
                    runOnCheck = false
                    mustRunAfterSuites = listOf("unitTest")

                    // One fork: an integration suite usually shares an external service.
                    maxParallelForks = 1
                    timeoutMinutes = 30L

                    environment = mapOf("EXAMPLE_INTEGRATION" to "true")

                    dependencies {
                        implementation("org.junit.jupiter:junit-jupiter:6.1.3")
                    }
                }

                register("contractTest") {
                    runOnCheck = false
                    includeTags = listOf("contract")

                    dependencies {
                        implementation("org.junit.jupiter:junit-jupiter:6.1.3")
                    }
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
