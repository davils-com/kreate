package com.davils.kreate.module.project

import org.gradle.api.Project

internal fun Project.addRepositories() {
    repositories.apply {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}