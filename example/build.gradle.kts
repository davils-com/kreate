import java.time.Year

plugins {
    alias(libs.plugins.kreate)
    id("dev.detekt") version "2.0.0-alpha.3"
    kotlin("jvm") version "2.3.21"
}

group = "com.example"

kreate {
    platform {
        javaVersion = JavaVersion.VERSION_25
        explicitApi = true
        allWarningsAsErrors = false

        multiplatform {
            cInterop {
                enabled = false
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
            }
        }
    }

    project {
        name = "Example"
        description = "Example project"

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
            enabled = false
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
        }

        detekt {
            enabled = true
            buildUponDefaultConfig = true
            allRules = true
            config = rootProject.file("detekt.yaml")

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

        trivy {
            enabled = true
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

dependencyLocking {
    lockAllConfigurations()
}
