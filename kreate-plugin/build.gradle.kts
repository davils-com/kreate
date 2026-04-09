plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
}

gradlePlugin {
    plugins {
        create("kreate") {
            id = "com.davils.kreate"
            implementationClass = "com.davils.kreate.Kreate"
        }
    }
}

val targetJavaVersion = JavaVersion.VERSION_21
java {
    sourceCompatibility = targetJavaVersion
    targetCompatibility = targetJavaVersion
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    explicitApi()
    jvmToolchain(targetJavaVersion.majorVersion.toInt())
}