plugins {
    alias(libs.plugins.kreate)
    kotlin("jvm") version "2.3.20"
}

kreate {
    platform {
        javaVersion = JavaVersion.VERSION_25
        explicitApi = true
        allWarningsAsErrors = false

        multiplatform {

        }
    }

    project {
        name = "Example"
        description = "Example project"

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
        }
    }
}
