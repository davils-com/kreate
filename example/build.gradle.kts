plugins {
    alias(libs.plugins.kreate)
    kotlin("jvm") version "2.3.20"
}

kreate {
    platform {
        javaVersion = JavaVersion.VERSION_25
        explicitApi = true
        allWarningsAsErrors = false
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
    }
}
