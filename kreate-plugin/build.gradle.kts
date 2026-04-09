import com.davils.buildsrc.Project

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = Project.Identity.GROUP.lowercase()


repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(libs.bundles.kreate.plugin)
}

gradlePlugin {
    vcsUrl = Project.VersionControl.SCM_URL
    website = Project.Organization.WEBSITE_URL

    plugins {
        create(Project.Identity.NAME.lowercase()) {
            id = "${Project.Identity.GROUP.lowercase()}.${Project.Identity.NAME.lowercase()}"
            description = Project.Identity.DESCRIPTION
            displayName = Project.Identity.NAME
            implementationClass = "${Project.Identity.GROUP}.${Project.Identity.NAME.lowercase()}.${Project.Identity.NAME}"
            tags = listOf("creation", "davils", "setup", "compatibility", "constants", "kotlin", "cinterop", "multiplatform")
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