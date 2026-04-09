plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

kotlin {
    explicitApi()
    compilerOptions {
        allWarningsAsErrors = true
    }
}